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
import com.jd.live.agent.core.plugin.definition.InterceptorAdaptor;
import com.jd.live.agent.governance.exception.ServiceError;
import com.jd.live.agent.governance.invoke.InvocationContext;
import com.jd.live.agent.governance.invoke.OutboundInvocation.HttpOutboundInvocation;
import com.jd.live.agent.governance.registry.Registry;
import com.jd.live.agent.governance.registry.ServiceEndpoint;
import com.jd.live.agent.plugin.router.springcloud.v1.cluster.FeignWebCluster;
import com.jd.live.agent.plugin.router.springcloud.v1.exception.SpringOutboundThrower;
import com.jd.live.agent.plugin.router.springcloud.v1.exception.feign.FeignThrowerFactory;
import com.jd.live.agent.plugin.router.springcloud.v1.exception.feign.InternalServerError;
import com.jd.live.agent.plugin.router.springcloud.v1.request.FeignOutboundRequest;
import com.jd.live.agent.plugin.router.springcloud.v1.request.FeignWebClusterRequest;
import com.jd.live.agent.plugin.router.springcloud.v1.request.FeignWebOutboundRequest;
import com.jd.live.agent.plugin.router.springcloud.v1.response.FeignClusterResponse;
import feign.FeignException;
import feign.Request;
import feign.Response;

import java.io.IOException;
import java.net.URI;
import java.util.List;

import static com.jd.live.agent.plugin.router.springcloud.v1.request.FeignOutboundRequest.createRequest;

/**
 * FeignWebClusterInterceptor
 */
public class FeignWebClusterInterceptor extends InterceptorAdaptor {

    private final InvocationContext context;

    private final Registry registry;

    private final SpringOutboundThrower<FeignException, FeignOutboundRequest> thrower = new SpringOutboundThrower<>(new FeignThrowerFactory<>());

    public FeignWebClusterInterceptor(InvocationContext context, Registry registry) {
        this.context = context;
        this.registry = registry;
    }

    @Override
    public void onEnter(ExecutableContext ctx) {
        MethodContext mc = (MethodContext) ctx;
        Object[] arguments = mc.getArguments();
        Request request = (Request) arguments[0];
        URI uri = URI.create(request.url());
        String service = context.getService(uri);
        try {
            List<ServiceEndpoint> endpoints = registry.subscribeAndGet(service, 5000, (message, e) ->
                    new InternalServerError(message));
            if (endpoints == null || endpoints.isEmpty()) {
                // Failed to convert microservice, fallback to domain reques
                return;
            }
        } catch (Throwable e) {
            mc.skipWithThrowable(e);
            return;
        }
        if (context.isFlowControlEnabled()) {
            // cluster invocation
            request(request, service, uri, arguments, mc);
        } else {
            route(request, service, uri, arguments, mc);
        }
    }

    /**
     * Routes the request through the governance framework and updates the URI.
     *
     * @param request   the Feign request
     * @param service   the target service name
     * @param uri       the request URI
     * @param arguments the method arguments to modify
     * @param mc        the method context for handling exceptions
     */
    private void route(Request request, String service, URI uri, Object[] arguments, MethodContext mc) {
        FeignWebOutboundRequest ror = new FeignWebOutboundRequest(request, service, uri);
        HttpOutboundInvocation<FeignWebOutboundRequest> invocation = new HttpOutboundInvocation<>(ror, context);
        // route invocation
        try {
            arguments[0] = createRequest(uri, request, context.route(invocation));
        } catch (Throwable e) {
            mc.skipWithThrowable(thrower.createException(e, ror));
        }
    }

    /**
     * Executes the request through cluster management with load balancing and fault tolerance.
     *
     * @param request   the Feign request
     * @param service   the target service name
     * @param uri       the request URI
     * @param arguments the method arguments
     * @param mc        the method context for handling results and exceptions
     */
    private void request(Request request, String service, URI uri, Object[] arguments, MethodContext mc) {
        FeignWebClusterRequest ror = new FeignWebClusterRequest(request, service, uri, registry, endpoint -> {
            // invoke a endpoint
            arguments[0] = createRequest(uri, request, endpoint);
            try {
                return (Response) mc.invokeOrigin();
            } catch (IOException e) {
                throw e;
            } catch (Exception e) {
                throw new IOException(e.getMessage(), e);
            }
        });
        HttpOutboundInvocation<FeignWebClusterRequest> invocation = new HttpOutboundInvocation<>(ror, context);
        FeignClusterResponse response = FeignWebCluster.INSTANCE.request(invocation);
        ServiceError error = response.getError();
        if (error != null && !error.isServerError()) {
            mc.skipWithThrowable(error.getThrowable());
        } else {
            mc.skipWithResult(response.getResponse());
        }
    }
}
