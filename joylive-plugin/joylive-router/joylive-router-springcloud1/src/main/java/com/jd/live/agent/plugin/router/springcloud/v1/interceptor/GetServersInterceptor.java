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
package com.jd.live.agent.plugin.router.springcloud.v1.interceptor;

import com.jd.live.agent.bootstrap.bytekit.context.ExecutableContext;
import com.jd.live.agent.bootstrap.bytekit.context.MethodContext;
import com.jd.live.agent.bootstrap.logger.Logger;
import com.jd.live.agent.bootstrap.logger.LoggerFactory;
import com.jd.live.agent.bootstrap.util.type.FieldAccessor;
import com.jd.live.agent.bootstrap.util.type.FieldAccessorFactory;
import com.jd.live.agent.core.plugin.definition.InterceptorAdaptor;
import com.jd.live.agent.core.util.type.ClassUtils;
import com.jd.live.agent.governance.context.RequestContext;
import com.jd.live.agent.governance.invoke.InvocationContext;
import com.jd.live.agent.governance.invoke.OutboundInvocation;
import com.jd.live.agent.governance.invoke.OutboundInvocation.HttpOutboundInvocation;
import com.jd.live.agent.governance.registry.ServiceEndpoint;
import com.jd.live.agent.governance.registry.SimpleServiceRegistry;
import com.jd.live.agent.governance.request.HttpRequest.HttpOutboundRequest;
import com.jd.live.agent.plugin.router.springcloud.v1.exception.SpringOutboundThrower;
import com.jd.live.agent.plugin.router.springcloud.v1.exception.status.StatusThrowerFactory;
import com.jd.live.agent.plugin.router.springcloud.v1.instance.EndpointServer;
import com.jd.live.agent.plugin.router.springcloud.v1.instance.RibbonEndpoint;
import com.jd.live.agent.plugin.router.springcloud.v1.request.FeignCloudOutboundRequest;
import com.jd.live.agent.plugin.router.springcloud.v1.request.HttpClientOutboundRequest;
import com.netflix.client.ClientRequest;
import com.netflix.loadbalancer.BaseLoadBalancer;
import com.netflix.loadbalancer.Server;
import feign.Request;
import org.springframework.core.NestedRuntimeException;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatus;

import java.util.List;

import static com.jd.live.agent.core.util.CollectionUtils.toList;
import static com.jd.live.agent.plugin.router.springcloud.v1.instance.RibbonEndpoint.ATTRIBUTE_CLIENT_REQUEST;

/**
 * BaseLoadBalancerInterceptor for live and lane route
 */
public class GetServersInterceptor extends InterceptorAdaptor {

    private static final Logger logger = LoggerFactory.getLogger(GetServersInterceptor.class);

    private final InvocationContext context;

    private final SpringOutboundThrower<NestedRuntimeException, HttpOutboundRequest> thrower = new SpringOutboundThrower<>(new StatusThrowerFactory<>());

    public GetServersInterceptor(InvocationContext context) {
        this.context = context;
    }

    @Override
    public void onSuccess(ExecutableContext ctx) {
        MethodContext mc = (MethodContext) ctx;
        List<Server> servers = mc.getResult();
        if (servers == null) {
            return;
        }
        BaseLoadBalancer balancer = (BaseLoadBalancer) mc.getTarget();
        String service = balancer.getName();
        Object clientRequest = RequestContext.getAttribute(ATTRIBUTE_CLIENT_REQUEST);
        HttpOutboundRequest request = Accessor.build(clientRequest, service);
        if (request == null) {
            return;
        }
        OutboundInvocation<HttpOutboundRequest> invocation = new HttpOutboundInvocation<>(request, context);
        try {
            List<ServiceEndpoint> endpoints = context.routes(invocation, new SimpleServiceRegistry(service, () -> toList(servers, s -> new RibbonEndpoint(service, s))));
            mc.setResult(toList(endpoints, this::getServer));
        } catch (Throwable e) {
            logger.error("Exception occurred when routing, caused by " + e.getMessage(), e);
            Throwable throwable = thrower.createException(e, invocation.getRequest());
            if (throwable instanceof RuntimeException) {
                throw (RuntimeException) throwable;
            } else {
                throw thrower.createException(invocation.getRequest(), HttpStatus.SERVICE_UNAVAILABLE, throwable.getMessage(), throwable);
            }
        }
    }

    private Server getServer(ServiceEndpoint endpoint) {
        return endpoint instanceof RibbonEndpoint ? ((RibbonEndpoint) endpoint).getServer() : new EndpointServer(endpoint);
    }

    private static final class Accessor {

        private static final Class<?> ribbonRequestType = ClassUtils.loadClass("org.springframework.cloud.netflix.feign.ribbon.FeignLoadBalancer$RibbonRequest", ClientRequest.class.getClassLoader());

        private static final FieldAccessor ribbonRequestAccessor = FieldAccessorFactory.getAccessor(ribbonRequestType, "request");

        public static HttpOutboundRequest build(Object request, String service) {
            if (request instanceof HttpRequest) {
                return new HttpClientOutboundRequest((HttpRequest) request, service);
            } else if (ribbonRequestType != null && ribbonRequestType.isInstance(request)) {
                Request feignRequest = (Request) ribbonRequestAccessor.get(request);
                return new FeignCloudOutboundRequest(feignRequest, service);
            }
            return null;
        }

    }
}
