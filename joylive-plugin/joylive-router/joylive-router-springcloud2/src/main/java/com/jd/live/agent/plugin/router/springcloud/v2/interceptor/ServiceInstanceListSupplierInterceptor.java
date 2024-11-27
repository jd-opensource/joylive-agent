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
package com.jd.live.agent.plugin.router.springcloud.v2.interceptor;

import com.jd.live.agent.bootstrap.bytekit.context.ExecutableContext;
import com.jd.live.agent.bootstrap.bytekit.context.MethodContext;
import com.jd.live.agent.bootstrap.logger.Logger;
import com.jd.live.agent.bootstrap.logger.LoggerFactory;
import com.jd.live.agent.core.plugin.definition.InterceptorAdaptor;
import com.jd.live.agent.core.util.CollectionUtils;
import com.jd.live.agent.governance.context.RequestContext;
import com.jd.live.agent.governance.context.bag.Carrier;
import com.jd.live.agent.governance.invoke.InvocationContext;
import com.jd.live.agent.governance.invoke.OutboundInvocation;
import com.jd.live.agent.governance.invoke.OutboundInvocation.GatewayHttpOutboundInvocation;
import com.jd.live.agent.governance.invoke.OutboundInvocation.HttpOutboundInvocation;
import com.jd.live.agent.governance.request.HttpRequest.HttpOutboundRequest;
import com.jd.live.agent.plugin.router.springcloud.v2.exception.SpringOutboundThrower;
import com.jd.live.agent.plugin.router.springcloud.v2.instance.SpringEndpoint;
import com.jd.live.agent.plugin.router.springcloud.v2.request.BlockingOutboundRequest;
import com.jd.live.agent.plugin.router.springcloud.v2.request.FeignOutboundRequest;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.Request;
import org.springframework.cloud.loadbalancer.core.DelegatingServiceInstanceListSupplier;
import org.springframework.cloud.loadbalancer.core.ServiceInstanceListSupplier;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatus;
import reactor.core.publisher.Flux;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * ServiceInstanceListSupplierInterceptor
 *
 * @author Zhiguo.Chen
 * @since 1.5.0
 */
public class ServiceInstanceListSupplierInterceptor extends InterceptorAdaptor {

    private static final Logger logger = LoggerFactory.getLogger(ServiceInstanceListSupplierInterceptor.class);

    private static final ThreadLocal<Long> LOCK = new ThreadLocal<>();

    private final InvocationContext context;

    private final Set<String> disableDiscovery;

    public ServiceInstanceListSupplierInterceptor(InvocationContext context, Set<String> disableDiscovery) {
        this.context = context;
        this.disableDiscovery = disableDiscovery == null ? new HashSet<>() : new HashSet<>(disableDiscovery);
        if (context.isLiveEnabled()) {
            this.disableDiscovery.add("org.springframework.cloud.loadbalancer.core.SameInstancePreferenceServiceInstanceListSupplier");
            this.disableDiscovery.add("org.springframework.cloud.loadbalancer.core.ZonePreferenceServiceInstanceListSupplier");
        }
        if (context.isFlowControlEnabled()) {
            this.disableDiscovery.add("org.springframework.cloud.loadbalancer.core.SameInstancePreferenceServiceInstanceListSupplier");
        }
    }

    @Override
    public void onEnter(ExecutableContext ctx) {
        MethodContext mc = (MethodContext) ctx;
        ServiceInstanceListSupplier target = (ServiceInstanceListSupplier) ctx.getTarget();
        if (target instanceof DelegatingServiceInstanceListSupplier
                && disableDiscovery != null
                && !disableDiscovery.isEmpty()
                && disableDiscovery.contains(target.getClass().getName())) {
            // disable
            DelegatingServiceInstanceListSupplier delegating = (DelegatingServiceInstanceListSupplier) target;
            mc.skipWithResult(delegating.getDelegate().get());
        } else if (!context.isFlowControlEnabled() && LOCK.get() == null) {
            // Prevent duplicate calls
            LOCK.set(ctx.getId());
        }
    }

    @Override
    public void onExit(ExecutableContext ctx) {
        if (!context.isFlowControlEnabled() && LOCK.get() == ctx.getId()) {
            LOCK.remove();
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onSuccess(ExecutableContext ctx) {
        if (!context.isFlowControlEnabled() && LOCK.get() == ctx.getId()) {
            MethodContext mc = (MethodContext) ctx;
            Object result = mc.getResult();
            Flux<List<ServiceInstance>> flux = (Flux<List<ServiceInstance>>) result;
            OutboundInvocation<HttpOutboundRequest> invocation = buildInvocation();
            if (invocation != null) {
                mc.setResult(flux.map(instances -> route(invocation, instances)));
            }
        }
    }

    /**
     * Routes the given outbound invocation to a service instance.
     *
     * @param invocation The outbound invocation to route.
     * @param instances  The list of service instances to choose from.
     * @return A list containing the selected service instance.
     */
    private List<ServiceInstance> route(OutboundInvocation<HttpOutboundRequest> invocation, List<ServiceInstance> instances) {
        try {
            invocation.setInstances(CollectionUtils.convert(instances, SpringEndpoint::new));
            SpringEndpoint endpoint = context.route(invocation);
            return Collections.singletonList(endpoint.getInstance());
        } catch (Throwable e) {
            logger.error("Exception occurred when routing, caused by " + e.getMessage(), e);
            SpringOutboundThrower<HttpOutboundRequest> thrower = new SpringOutboundThrower<>();
            Throwable throwable = thrower.createException(e, invocation.getRequest());
            if (throwable instanceof RuntimeException) {
                throw (RuntimeException) throwable;
            } else {
                throw SpringOutboundThrower.createException(HttpStatus.SERVICE_UNAVAILABLE, throwable.getMessage(), throwable);
            }
        }
    }

    /**
     * Builds an {@link OutboundInvocation} based on the provided {@link Request}.
     *
     * @return An instance of {@code OutboundInvocation} specific to the type of the request, or {@code null}
     * if the request type is not supported.
     */
    private OutboundInvocation<HttpOutboundRequest> buildInvocation() {
        Object request = RequestContext.removeAttribute(Carrier.ATTRIBUTE_REQUEST);
        String serviceName = RequestContext.removeAttribute(Carrier.ATTRIBUTE_SERVICE_ID);
        if (request instanceof HttpRequest) {
            return createOutlet(new BlockingOutboundRequest((HttpRequest) request, serviceName));
        } else if (request instanceof feign.Request) {
            return createOutlet(new FeignOutboundRequest((feign.Request) request, serviceName));
        }
        return null;
    }

    /**
     * Creates an {@link OutboundInvocation} for the given {@link HttpOutboundRequest}.
     *
     * @param request The HTTP outbound request for which to create the {@code OutboundInvocation}.
     * @return An instance of {@code OutboundInvocation} tailored for gateway or non-gateway operation.
     */
    private OutboundInvocation<HttpOutboundRequest> createOutlet(HttpOutboundRequest request) {
        Boolean gateway = RequestContext.getAttribute(Carrier.ATTRIBUTE_GATEWAY);
        gateway = gateway == null ? context.getApplication().getService().isGateway() : gateway;
        return gateway ? new GatewayHttpOutboundInvocation<>(request, context) :
                new HttpOutboundInvocation<>(request, context);
    }

}
