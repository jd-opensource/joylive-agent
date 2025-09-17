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
import com.jd.live.agent.bootstrap.bytekit.context.MethodContext;
import com.jd.live.agent.core.plugin.definition.InterceptorAdaptor;
import com.jd.live.agent.governance.context.RequestContext;
import com.jd.live.agent.governance.exception.ServiceError;
import com.jd.live.agent.governance.invoke.InvocationContext;
import com.jd.live.agent.governance.invoke.InvocationContext.HttpForwardContext;
import com.jd.live.agent.governance.invoke.OutboundInvocation.HttpOutboundInvocation;
import com.jd.live.agent.governance.registry.Registry;
import com.jd.live.agent.governance.registry.ServiceEndpoint;
import com.jd.live.agent.governance.request.HostTransformer;
import com.jd.live.agent.plugin.router.springcloud.v4.cluster.FeignClientCluster;
import com.jd.live.agent.plugin.router.springcloud.v4.exception.SpringOutboundThrower;
import com.jd.live.agent.plugin.router.springcloud.v4.exception.feign.FeignThrowerFactory;
import com.jd.live.agent.plugin.router.springcloud.v4.request.FeignClientClusterRequest;
import com.jd.live.agent.plugin.router.springcloud.v4.request.FeignClientForwardRequest;
import com.jd.live.agent.plugin.router.springcloud.v4.request.FeignOutboundRequest;
import com.jd.live.agent.plugin.router.springcloud.v4.response.FeignClusterResponse;
import feign.FeignException;
import feign.FeignException.InternalServerError;
import feign.Request;
import feign.Response;
import org.springframework.http.client.support.HttpAccessor;

import java.io.IOException;
import java.net.URI;
import java.util.List;

import static com.jd.live.agent.core.util.type.ClassUtils.loadClass;
import static com.jd.live.agent.governance.request.Request.KEY_CLOUD_REQUEST;
import static com.jd.live.agent.plugin.router.springcloud.v4.condition.ConditionalOnSpringCloud4Enabled.TYPE_SPRING_CLOUD_APPLICATION;
import static com.jd.live.agent.plugin.router.springcloud.v4.request.FeignOutboundRequest.createRequest;

/**
 * FeignClientInterceptor
 */
public class FeignClientInterceptor extends InterceptorAdaptor {

    private final InvocationContext context;

    private final Registry registry;

    public FeignClientInterceptor(InvocationContext context) {
        this.context = context;
        this.registry = context.getRegistry();
    }

    @Override
    public void onEnter(ExecutableContext ctx) {
        MethodContext mc = (MethodContext) ctx;
        Request request = ctx.getArgument(0);
        if (Accessor.isCloudEnabled()) {
            // with spring cloud
            if (!RequestContext.hasAttribute(KEY_CLOUD_REQUEST)) {
                // Handle multi-active and lane domains
                forward(request, URI.create(request.url()), mc);
            }
        } else {
            // only spring boot
            URI uri = URI.create(request.url());
            // determine whether is a microservice request
            String service = context.isMicroserviceTransformEnabled() ? context.getService(uri) : null;
            if (service != null && !service.isEmpty()) {
                // Convert regular spring web requests to microservice calls
                invoke(request, service, uri, mc);
            } else {
                // Handle multi-active and lane domains
                forward(request, uri, mc);
            }
        }
    }

    /**
     * Forwards the request through HTTP routing and updates the executable context if URI changes.
     *
     * @param request the original request to forward
     * @param uri     the target URI
     * @param mc      the executable context to update
     */
    private void forward(Object request, URI uri, MethodContext mc) {
        // Parameter request cannot be declared as Request, as it will cause class loading exceptions.
        HostTransformer transformer = context.getHostTransformer(uri.getHost());
        if (transformer != null) {
            Request req = (Request) request;
            FeignClientForwardRequest fr = new FeignClientForwardRequest(req, uri, transformer);
            try {
                URI newUri = HttpForwardContext.of(context).route(fr);
                if (newUri != uri) {
                    mc.setArgument(0, createRequest(newUri, req));
                }
            } catch (Throwable e) {
                mc.skipWithThrowable(Accessor.thrower.createException(e, fr));
            }
        }
    }

    /**
     * Executes the request through cluster management with load balancing and fault tolerance.
     *
     * @param request   the Feign request
     * @param service   the target service name
     * @param uri       the request URI
     * @param mc        the method context for handling results and exceptions
     */
    private void invoke(Object request, String service, URI uri, MethodContext mc) {
        // Parameter request cannot be declared as Request, as it will cause class loading exceptions.
        if (!subscribe(request, service)) {
            return;
        }
        Request req = (Request) request;
        // invoke
        FeignClientClusterRequest fr = new FeignClientClusterRequest(req, service, uri, registry, endpoint -> {
            // invoke a endpoint
            mc.setArgument(0, createRequest(uri, req, endpoint));
            try {
                return (Response) mc.invokeOrigin();
            } catch (IOException e) {
                throw e;
            } catch (Exception e) {
                throw new IOException(e.getMessage(), e);
            }
        });
        try {
            FeignClusterResponse response = FeignClientCluster.INSTANCE.request(new HttpOutboundInvocation<>(fr, context));
            ServiceError error = response.getError();
            if (error != null && !error.isServerError()) {
                mc.skipWithThrowable(error.getThrowable());
            } else {
                mc.skipWithResult(response.getResponse());
            }
        } catch (Throwable e) {
            mc.skipWithThrowable(Accessor.thrower.createException(e, fr));
        }
    }

    /**
     * Subscribes to service endpoints for the specified service.
     *
     * @param request the HTTP request
     * @param service the service name to subscribe
     * @return true if subscription succeeded and endpoints are available, false otherwise
     */
    private boolean subscribe(Object request, String service) {
        // Parameter request cannot be declared as Request, as it will cause class loading exceptions.
        // subscribe service endpoint and governance policy.
        Request req = (Request) request;
        try {
            List<ServiceEndpoint> endpoints = registry.subscribeAndGet(service, 5000, (message, e) ->
                    new InternalServerError(message, req, req.body(), req.headers()));
            if (endpoints == null || endpoints.isEmpty()) {
                // Failed to convert microservice, fallback to domain reques
                return false;
            }
            return true;
        } catch (Throwable e) {
            return false;
        }
    }

    /**
     * Utility class for detecting Spring Cloud environment and load balancer configuration.
     */
    private static class Accessor {

        // spring cloud 3+
        private static final Class<?> lbType = loadClass(TYPE_SPRING_CLOUD_APPLICATION, HttpAccessor.class.getClassLoader());

        private static final SpringOutboundThrower<FeignException, FeignOutboundRequest> thrower = new SpringOutboundThrower<>(new FeignThrowerFactory<>());

        /**
         * Checks if Spring Cloud is available in the classpath.
         *
         * @return true if Spring Cloud is present, false otherwise
         */
        public static boolean isCloudEnabled() {
            return lbType != null;
        }
    }

}
