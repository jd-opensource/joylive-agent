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
import com.jd.live.agent.bootstrap.bytekit.context.MethodContext;
import com.jd.live.agent.governance.context.RequestContext;
import com.jd.live.agent.governance.context.bag.Carrier;
import com.jd.live.agent.governance.interceptor.AbstractInterceptor.AbstractHttpRouteInterceptor;
import com.jd.live.agent.governance.invoke.InvocationContext;
import com.jd.live.agent.governance.invoke.OutboundInvocation;
import com.jd.live.agent.governance.invoke.OutboundInvocation.HttpOutboundInvocation;
import com.jd.live.agent.governance.invoke.filter.RouteFilter;
import com.jd.live.agent.governance.request.HttpRequest.HttpOutboundRequest;
import com.jd.live.agent.plugin.router.springcloud.v3.instance.SpringEndpoint;
import com.jd.live.agent.plugin.router.springcloud.v3.request.ReactiveOutboundRequest;
import com.jd.live.agent.plugin.router.springcloud.v3.request.RequestDataOutboundRequest;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.Request;
import org.springframework.cloud.client.loadbalancer.RequestDataContext;
import org.springframework.cloud.client.loadbalancer.RetryableRequestContext;
import org.springframework.http.HttpRequest;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.stream.Collectors;

/**
 * ServiceInstanceListSupplierInterceptor
 *
 * @author Zhiguo.Chen
 * @since 1.0.0
 */
public class ServiceInstanceListSupplierInterceptor extends AbstractHttpRouteInterceptor<HttpOutboundRequest> {

    private static final ThreadLocal<Boolean> LOCK = new ThreadLocal<>();

    private static final String LOCKED = "LOCKED";

    public ServiceInstanceListSupplierInterceptor(InvocationContext context, List<RouteFilter> filters) {
        super(context, filters);
    }

    @Override
    public void onEnter(ExecutableContext ctx) {
        if (LOCK.get() == null) {
            // Prevent duplicate calls
            LOCK.set(Boolean.TRUE);
            ctx.setAttribute(LOCKED, Boolean.TRUE);
        }
    }

    @Override
    public void onExit(ExecutableContext ctx) {
        if (ctx.getAttribute(LOCKED) != null) {
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
        if (ctx.getAttribute(LOCKED) != null) {
            MethodContext mc = (MethodContext) ctx;
            Object[] arguments = ctx.getArguments();
            Object result = mc.getResult();
            Flux<List<ServiceInstance>> flux = (Flux<List<ServiceInstance>>) result;
            HttpOutboundInvocation<HttpOutboundRequest> invocation = buildInvocation((Request<?>) arguments[0]);
            if (invocation != null) {
                mc.setResult(flux.map(instances -> {
                    invocation.setInstances(instances.stream().map(SpringEndpoint::new).collect(Collectors.toList()));
                    routing(invocation);
                    return invocation.getEndpoints().stream().map(endpoint -> ((SpringEndpoint) endpoint).getInstance()).collect(Collectors.toList());
                }));
            }
        }
    }

    @Override
    protected HttpOutboundInvocation<HttpOutboundRequest> createOutlet(HttpOutboundRequest request) {
        Boolean gateway = RequestContext.getAttribute(Carrier.ATTRIBUTE_GATEWAY);
        gateway = gateway == null ? context.getApplication().getService().isGateway() : gateway;
        return gateway ? new OutboundInvocation.GatewayHttpOutboundInvocation<>(request, context) :
                super.createOutlet(request);
    }

    private HttpOutboundInvocation<HttpOutboundRequest> buildInvocation(Request<?> request) {
        Object context = request.getContext();
        if (context instanceof RequestDataContext) {
            return createOutlet(createOutboundRequest((RequestDataContext) context));
        } else if (request instanceof HttpRequest) {
            return createOutlet(new ReactiveOutboundRequest((HttpRequest) request,
                    RequestContext.getAttribute(Carrier.ATTRIBUTE_SERVICE_ID)));
        }
        return null;
    }

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
