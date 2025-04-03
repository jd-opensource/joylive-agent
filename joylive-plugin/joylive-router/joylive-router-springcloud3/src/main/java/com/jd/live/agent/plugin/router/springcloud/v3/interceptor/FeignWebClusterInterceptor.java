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
import com.jd.live.agent.core.plugin.definition.InterceptorAdaptor;
import com.jd.live.agent.governance.exception.ServiceError;
import com.jd.live.agent.governance.invoke.InvocationContext;
import com.jd.live.agent.governance.invoke.OutboundInvocation.HttpOutboundInvocation;
import com.jd.live.agent.governance.registry.Registry;
import com.jd.live.agent.plugin.router.springcloud.v3.cluster.FeignWebCluster;
import com.jd.live.agent.plugin.router.springcloud.v3.exception.SpringOutboundThrower;
import com.jd.live.agent.plugin.router.springcloud.v3.exception.feign.FeignThrowerFactory;
import com.jd.live.agent.plugin.router.springcloud.v3.request.FeignOutboundRequest;
import com.jd.live.agent.plugin.router.springcloud.v3.request.FeignWebClusterRequest;
import com.jd.live.agent.plugin.router.springcloud.v3.request.FeignWebOutboundRequest;
import com.jd.live.agent.plugin.router.springcloud.v3.response.FeignClusterResponse;
import feign.FeignException;
import feign.FeignException.InternalServerError;
import feign.Request;
import feign.Response;

import java.io.IOException;
import java.net.URI;

import static com.jd.live.agent.plugin.router.springcloud.v3.request.FeignOutboundRequest.createRequest;

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
        if (service == null || service.isEmpty()) {
            // none governance service
            return;
        }
        Throwable throwable = registry.subscribe(service, (message, e) ->
                new InternalServerError(message, request, request.body(), request.headers()));
        if (throwable != null) {
            // governance policy is not ready
            mc.skipWithThrowable(throwable);
        } else if (context.isFlowControlEnabled()) {
            // cluster invocation
            FeignWebClusterRequest ror = new FeignWebClusterRequest(request, service, uri, registry.getEndpoints(service), endpoint -> {
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
        } else {
            FeignWebOutboundRequest ror = new FeignWebOutboundRequest(request, service, uri);
            HttpOutboundInvocation<FeignWebOutboundRequest> invocation = new HttpOutboundInvocation<>(ror, context);
            // route invocation
            try {
                arguments[0] = createRequest(uri, request, context.route(invocation, registry.getEndpoints(service)));
            } catch (Throwable e) {
                mc.skipWithThrowable(thrower.createException(e, ror));
            }
        }
    }
}
