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
package com.jd.live.agent.plugin.router.springcloud.v3.interceptor;

import com.jd.live.agent.bootstrap.bytekit.context.ExecutableContext;
import com.jd.live.agent.bootstrap.bytekit.context.LockContext;
import com.jd.live.agent.bootstrap.bytekit.context.MethodContext;
import com.jd.live.agent.bootstrap.logger.Logger;
import com.jd.live.agent.bootstrap.logger.LoggerFactory;
import com.jd.live.agent.core.plugin.definition.InterceptorAdaptor;
import com.jd.live.agent.governance.context.RequestContext;
import com.jd.live.agent.governance.context.bag.Carrier;
import com.jd.live.agent.governance.invoke.InvocationContext;
import com.jd.live.agent.governance.invoke.OutboundInvocation;
import com.jd.live.agent.governance.invoke.OutboundInvocation.GatewayHttpOutboundInvocation;
import com.jd.live.agent.governance.invoke.OutboundInvocation.HttpOutboundInvocation;
import com.jd.live.agent.governance.registry.ServiceEndpoint;
import com.jd.live.agent.governance.registry.SimpleServiceRegistry;
import com.jd.live.agent.governance.request.HttpRequest.HttpOutboundRequest;
import com.jd.live.agent.plugin.router.springcloud.v3.exception.SpringOutboundThrower;
import com.jd.live.agent.plugin.router.springcloud.v3.exception.status.StatusThrowerFactory;
import com.jd.live.agent.plugin.router.springcloud.v3.instance.EndpointInstance;
import com.jd.live.agent.plugin.router.springcloud.v3.instance.SpringEndpoint;
import com.jd.live.agent.plugin.router.springcloud.v3.request.BlockingCloudOutboundRequest;
import com.jd.live.agent.plugin.router.springcloud.v3.request.RequestDataOutboundRequest;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.Request;
import org.springframework.cloud.client.loadbalancer.RequestDataContext;
import org.springframework.cloud.client.loadbalancer.RetryableRequestContext;
import org.springframework.cloud.loadbalancer.core.DelegatingServiceInstanceListSupplier;
import org.springframework.cloud.loadbalancer.core.ServiceInstanceListSupplier;
import org.springframework.core.NestedRuntimeException;
import org.springframework.http.HttpRequest;
import reactor.core.publisher.Flux;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.jd.live.agent.core.util.CollectionUtils.singletonList;
import static com.jd.live.agent.core.util.CollectionUtils.toList;
import static com.jd.live.agent.plugin.router.springcloud.v3.instance.EndpointInstance.convert;

/**
 * ServiceInstanceListSupplierInterceptor
 *
 * @author Zhiguo.Chen
 * @since 1.0.0
 */
public class ServiceInstanceListSupplierInterceptor extends InterceptorAdaptor {

    private static final Logger logger = LoggerFactory.getLogger(ServiceInstanceListSupplierInterceptor.class);

    private static final LockContext lock = new LockContext.DefaultLockContext();

    private final InvocationContext context;

    private final Set<String> disableDiscovery;

    private final SpringOutboundThrower<NestedRuntimeException, HttpOutboundRequest> thrower = new SpringOutboundThrower<>(new StatusThrowerFactory<>());

    public ServiceInstanceListSupplierInterceptor(InvocationContext context, Set<String> disableDiscovery) {
        this.context = context;
        this.disableDiscovery = disableDiscovery == null ? new HashSet<>() : new HashSet<>(disableDiscovery);
        if (context.isLiveEnabled()) {
            this.disableDiscovery.add("org.springframework.cloud.loadbalancer.core.ZonePreferenceServiceInstanceListSupplier");
            this.disableDiscovery.add("org.springframework.cloud.loadbalancer.core.SubsetServiceInstanceListSupplier");
            this.disableDiscovery.add("org.springframework.cloud.loadbalancer.core.SameInstancePreferenceServiceInstanceListSupplier");
            this.disableDiscovery.add("org.springframework.cloud.loadbalancer.core.RequestBasedStickySessionServiceInstanceListSupplier");
        }
        if (context.isFlowControlEnabled()) {
            this.disableDiscovery.add("org.springframework.cloud.loadbalancer.core.SameInstancePreferenceServiceInstanceListSupplier");
            this.disableDiscovery.add("org.springframework.cloud.loadbalancer.core.RequestBasedStickySessionServiceInstanceListSupplier");
            this.disableDiscovery.add("org.springframework.cloud.loadbalancer.core.RetryAwareServiceInstanceListSupplier");
            this.disableDiscovery.add("org.springframework.cloud.loadbalancer.core.WeightedServiceInstanceListSupplier");
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
            mc.skipWithResult(delegating.getDelegate().get(ctx.getArgument(0)));
        } else if (!context.isFlowControlEnabled()) {
            // Prevent duplicate calls
            ctx.tryLock(lock);
        }
    }

    @Override
    public void onExit(ExecutableContext ctx) {
        if (!context.isFlowControlEnabled()) {
            ctx.unlock();
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onSuccess(ExecutableContext ctx) {
        if (!context.isFlowControlEnabled() && ctx.isLocked()) {
            MethodContext mc = (MethodContext) ctx;
            Object[] arguments = ctx.getArguments();
            Object result = mc.getResult();
            Flux<List<ServiceInstance>> flux = (Flux<List<ServiceInstance>>) result;
            OutboundInvocation<HttpOutboundRequest> invocation = buildInvocation((Request<?>) arguments[0]);
            if (invocation != null) {
                mc.setResult(flux.map(instances -> route(invocation, instances)));
            }
        }
    }

    /**
     * Routes the given outbound invocation to a service instance.
     *
     * @param invocation The outbound invocation to route.
     * @param system     The list of service instances to choose from.
     * @return A list containing the selected service instance.
     */
    private List<ServiceInstance> route(OutboundInvocation<HttpOutboundRequest> invocation, List<ServiceInstance> system) {
        try {
            String service = invocation.getRequest().getService();
            if (context.isFlowControlEnabled()) {
                ServiceEndpoint endpoint = context.route(invocation, new SimpleServiceRegistry(service, () -> toList(system, SpringEndpoint::new)));
                return singletonList(convert(endpoint));
            } else {
                List<ServiceEndpoint> endpoints = context.routes(invocation, new SimpleServiceRegistry(service, () -> toList(system, SpringEndpoint::new)));
                return toList(endpoints, EndpointInstance::convert);
            }
        } catch (Throwable e) {
            logger.error("Exception occurred when routing, caused by " + e.getMessage(), e);
            throw (NestedRuntimeException) thrower.createException(e, invocation.getRequest());
        }
    }

    /**
     * Builds an {@link OutboundInvocation} based on the provided {@link Request}.
     *
     * @param request The request from which to build the {@code OutboundInvocation}.
     * @return An instance of {@code OutboundInvocation} specific to the type of the request, or {@code null}
     * if the request type is not supported.
     */
    private OutboundInvocation<HttpOutboundRequest> buildInvocation(Request<?> request) {
        Object context = request.getContext();
        if (context instanceof RequestDataContext) {
            return createOutlet(createOutboundRequest((RequestDataContext) context));
        } else if (request instanceof HttpRequest) {
            return createOutlet(new BlockingCloudOutboundRequest((HttpRequest) request, RequestContext.getAttribute(Carrier.ATTRIBUTE_SERVICE_ID)));
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

    /**
     * Creates a {@link RequestDataContext} from a given {@link RequestDataContext}.
     *
     * @param context The request data context containing information needed to create the outbound request.
     * @return A newly created {@code RequestDataLbRequest} with context and potentially retry information.
     */
    private RequestDataOutboundRequest createOutboundRequest(RequestDataContext context) {
        RequestDataOutboundRequest result = new RequestDataOutboundRequest(context.getClientRequest(),
                RequestContext.getAttribute(Carrier.ATTRIBUTE_SERVICE_ID));

        if (context instanceof RetryableRequestContext) {
            ServiceInstance previousServiceInstance = ((RetryableRequestContext) context).getPreviousServiceInstance();
            if (previousServiceInstance != null) {
                result.addAttempt(previousServiceInstance.getInstanceId());
            }
        }
        return result;
    }
}
