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
import com.jd.live.agent.core.util.CollectionUtils;
import com.jd.live.agent.governance.context.RequestContext;
import com.jd.live.agent.governance.context.bag.Carrier;
import com.jd.live.agent.governance.invoke.InvocationContext;
import com.jd.live.agent.governance.invoke.OutboundInvocation;
import com.jd.live.agent.governance.invoke.OutboundInvocation.GatewayHttpOutboundInvocation;
import com.jd.live.agent.governance.invoke.OutboundInvocation.HttpOutboundInvocation;
import com.jd.live.agent.governance.request.HttpRequest.HttpOutboundRequest;
import com.jd.live.agent.plugin.router.springcloud.v4.cluster.SpringOutboundThrower;
import com.jd.live.agent.plugin.router.springcloud.v4.instance.SpringEndpoint;
import com.jd.live.agent.plugin.router.springcloud.v4.request.BlockingOutboundRequest;
import com.jd.live.agent.plugin.router.springcloud.v4.request.RequestDataOutboundRequest;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.Request;
import org.springframework.cloud.client.loadbalancer.RequestDataContext;
import org.springframework.cloud.client.loadbalancer.RetryableRequestContext;
import org.springframework.http.HttpRequest;
import reactor.core.publisher.Flux;

import java.util.Collections;
import java.util.List;

/**
 * ServiceInstanceListSupplierInterceptor
 *
 * @author Zhiguo.Chen
 * @since 1.0.0
 */
public class ServiceInstanceListSupplierInterceptor extends InterceptorAdaptor {

    private static final ThreadLocal<Long> LOCK = new ThreadLocal<>();

    private final InvocationContext context;

    public ServiceInstanceListSupplierInterceptor(InvocationContext context) {
        this.context = context;
    }

    @Override
    public void onEnter(ExecutableContext ctx) {
        if (LOCK.get() == null) {
            // Prevent duplicate calls
            LOCK.set(ctx.getId());
        }
    }

    @Override
    public void onExit(ExecutableContext ctx) {
        if (LOCK.get() == ctx.getId()) {
            LOCK.remove();
        }
    }

    /**
     * Enhanced logic after method execution
     *
     * @param ctx ExecutableContext
     * @see org.springframework.cloud.loadbalancer.core.ServiceInstanceListSupplier#get(org.springframework.cloud.client.loadbalancer.Request)
     */
    @SuppressWarnings("unchecked")
    @Override
    public void onSuccess(ExecutableContext ctx) {
        if (LOCK.get() == ctx.getId()) {
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
     * @param instances  The list of service instances to choose from.
     * @return A list containing the selected service instance.
     */
    private List<ServiceInstance> route(OutboundInvocation<HttpOutboundRequest> invocation, List<ServiceInstance> instances) {
        try {
            invocation.setInstances(CollectionUtils.convert(instances, SpringEndpoint::new));
            SpringEndpoint endpoint = context.route(invocation);
            return Collections.singletonList(endpoint.getInstance());
        } catch (Throwable e) {
            SpringOutboundThrower<HttpOutboundRequest> thrower = new SpringOutboundThrower<>();
            throw thrower.createException(e, invocation.getRequest());
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
            return createOutlet(new BlockingOutboundRequest((HttpRequest) request, RequestContext.getAttribute(Carrier.ATTRIBUTE_SERVICE_ID)));
        }
        return null;
    }

    /**
     * Creates an {@link OutboundInvocation} for the given {@link HttpOutboundRequest}.
     * <p>
     * This method checks if the current context is designated as a gateway and creates either a
     * {@link GatewayHttpOutboundInvocation} or a standard {@link HttpOutboundInvocation} accordingly.
     * The determination of the gateway status can come from the {@link RequestContext} or fallback to
     * the application's service configuration.
     * </p>
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
     * <p>
     * This method constructs a {@code RequestDataLbRequest} using the client request and service ID from
     * the provided context. If the context is an instance of {@link RetryableRequestContext}, it also
     * records any previous service instance attempts to the {@code RequestDataLbRequest} for retry logic.
     * </p>
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
