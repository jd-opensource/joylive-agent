/*
 * Copyright © ${year} ${owner} (${email})
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jd.live.agent.plugin.router.springcloud.v4.interceptor;

import com.jd.live.agent.bootstrap.bytekit.context.ExecutableContext;
import com.jd.live.agent.core.plugin.definition.InterceptorAdaptor;
import com.jd.live.agent.governance.invoke.InvocationContext;
import com.jd.live.agent.governance.invoke.InvocationContext.HttpForwardContext;
import com.jd.live.agent.governance.invoke.OutboundInvocation.HttpOutboundInvocation;
import com.jd.live.agent.governance.registry.Registry;
import com.jd.live.agent.governance.registry.ServiceEndpoint;
import com.jd.live.agent.governance.request.HostTransformer;
import com.jd.live.agent.governance.request.HttpRequest.HttpOutboundRequest;
import com.jd.live.agent.plugin.router.springcloud.v4.cluster.ReactiveClientCluster;
import com.jd.live.agent.plugin.router.springcloud.v4.exception.SpringOutboundThrower;
import com.jd.live.agent.plugin.router.springcloud.v4.exception.reactive.WebClientThrowerFactory;
import com.jd.live.agent.plugin.router.springcloud.v4.request.ReactiveClientClusterRequest;
import com.jd.live.agent.plugin.router.springcloud.v4.request.ReactiveClientForwardRequest;
import com.jd.live.agent.plugin.router.springcloud.v4.response.ReactiveClusterResponse;
import org.springframework.cloud.client.loadbalancer.reactive.DeferringLoadBalancerExchangeFilterFunction;
import org.springframework.cloud.client.loadbalancer.reactive.LoadBalancedExchangeFilterFunction;
import org.springframework.cloud.client.loadbalancer.reactive.RetryableLoadBalancerExchangeFilterFunction;
import org.springframework.http.client.support.HttpAccessor;
import org.springframework.web.reactive.function.client.*;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.List;
import java.util.concurrent.CompletionStage;

import static com.jd.live.agent.core.util.type.ClassUtils.loadClass;
import static com.jd.live.agent.plugin.router.springcloud.v4.condition.ConditionalOnSpringCloud4Enabled.TYPE_SPRING_CLOUD_APPLICATION;

/**
 * ReactiveClientInterceptor
 */
public class ReactiveClientInterceptor extends InterceptorAdaptor {

    private final InvocationContext context;

    private final Registry registry;

    public ReactiveClientInterceptor(InvocationContext context) {
        this.context = context;
        this.registry = context.getRegistry();
    }

    @Override
    public void onEnter(ExecutableContext ctx) {
        ExchangeFunction exchanger = ctx.getArgument(0);
        WebClient.Builder builder = ctx.getArgument(5);
        if (Accessor.isCloudEnabled()) {
            // with spring cloud
            if (!Accessor.isCloudClient(builder) && context.isSubdomainEnabled()) {
                // Handle multi-active and lane domains
                ctx.setArgument(0, exchanger.filter(this::forward));
            }
        } else if (context.isMicroserviceTransformEnabled()) {
            // Convert regular spring web requests to microservice calls
            ctx.setArgument(0, exchanger.filter(this::invoke));
        } else if (context.isSubdomainEnabled()) {
            // Handle multi-active and lane domains
            ctx.setArgument(0, exchanger.filter(this::forward));
        }
    }

    /**
     * Forwards the request with potential URI transformation.
     *
     * @param request the client request to forward
     * @param next    the next exchange function in the chain
     * @return the response mono
     */
    private Mono<ClientResponse> forward(Object request, Object next) {
        // Parameter request cannot be declared as Request, as it will cause class loading exceptions.
        // Parameter next cannot be declared as ExchangeFunction, as it will cause class loading exceptions.
        ClientRequest req = (ClientRequest) request;
        ExchangeFunction n = (ExchangeFunction) next;
        URI uri = req.url();
        HostTransformer transformer = context.getHostTransformer(uri.getHost());
        if (transformer != null) {
            ReactiveClientForwardRequest rr = new ReactiveClientForwardRequest(req, uri, transformer);
            try {
                URI newUri = HttpForwardContext.of(context).route(rr);
                if (newUri != uri) {
                    return n.exchange(ClientRequest.from(req).url(newUri).build());
                }
            } catch (Throwable e) {
                return Mono.error(Accessor.thrower.createException(e, rr));
            }
        }
        return n.exchange(req);
    }

    /**
     * Converts domain-based request to microservice invocation.
     * If service discovery fails, falls back to domain forwarding.
     *
     * @param request the client request
     * @param next    the exchange function for fallback
     * @return Mono containing the client response
     */
    private Mono<ClientResponse> invoke(Object request, Object next) {
        // Parameter request cannot be declared as Request, as it will cause class loading exceptions.
        // Parameter next cannot be declared as ExchangeFunction, as it will cause class loading exceptions.
        ClientRequest req = (ClientRequest) request;
        ExchangeFunction n = (ExchangeFunction) next;
        String service = context.getService(req.url());
        if (service == null || service.isEmpty()) {
            // Handle multi-active and lane domains
            return forward(request, next);
        }
        try {
            List<ServiceEndpoint> endpoints = registry.subscribeAndGet(service, 5000, (message, e) -> e);
            if (endpoints == null || endpoints.isEmpty()) {
                // Failed to convert microservice, fallback to domain request
                return n.exchange(req);
            }
        } catch (Throwable e) {
            // Failed to convert microservice, fallback to domain request
            return n.exchange(req);
        }
        ReactiveClientClusterRequest rr = new ReactiveClientClusterRequest(req, service, registry, n);
        HttpOutboundInvocation<ReactiveClientClusterRequest> invocation = new HttpOutboundInvocation<>(rr, context);
        CompletionStage<ReactiveClusterResponse> stage = ReactiveClientCluster.INSTANCE.invoke(invocation);
        return Mono.fromFuture(stage.toCompletableFuture().thenApply(ReactiveClusterResponse::getResponse));
    }

    /**
     * Utility class for detecting Spring Cloud environment and load balancer configuration.
     */
    private static class Accessor {

        // spring cloud 3+
        private static final Class<?> lbType = loadClass(TYPE_SPRING_CLOUD_APPLICATION, HttpAccessor.class.getClassLoader());

        private static final SpringOutboundThrower<WebClientException, HttpOutboundRequest> thrower = new SpringOutboundThrower<>(new WebClientThrowerFactory<>());

        /**
         * Checks if Spring Cloud is available in the classpath.
         *
         * @return true if Spring Cloud is present, false otherwise
         */
        public static boolean isCloudEnabled() {
            return lbType != null;
        }

        /**
         * Checks if the WebClient builder is configured for cloud load balancing.
         *
         * @param client the WebClient builder to check
         * @return true if the builder contains load balancing filters, false otherwise
         */
        public static boolean isCloudClient(Object client) {
            WebClient.Builder builder = (WebClient.Builder) client;
            final boolean[] result = new boolean[]{false};
            builder.filters(filters -> {
                for (ExchangeFilterFunction filter : filters) {
                    if (filter instanceof LoadBalancedExchangeFilterFunction
                            || filter instanceof DeferringLoadBalancerExchangeFilterFunction
                            || filter instanceof RetryableLoadBalancerExchangeFilterFunction) {
                        result[0] = true;
                        break;
                    }
                }
            });
            return result[0];
        }
    }
}
