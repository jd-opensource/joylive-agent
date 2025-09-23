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
import com.jd.live.agent.governance.context.RequestContext;
import com.jd.live.agent.governance.invoke.InvocationContext;
import com.jd.live.agent.governance.invoke.InvocationContext.HttpForwardContext;
import com.jd.live.agent.governance.invoke.OutboundInvocation.HttpOutboundInvocation;
import com.jd.live.agent.governance.registry.Registry;
import com.jd.live.agent.governance.request.HostTransformer;
import com.jd.live.agent.plugin.router.springcloud.v3.cluster.FeignClientCluster;
import com.jd.live.agent.plugin.router.springcloud.v3.exception.feign.FeignThrower;
import com.jd.live.agent.plugin.router.springcloud.v3.request.FeignClientClusterRequest;
import com.jd.live.agent.plugin.router.springcloud.v3.request.FeignClientForwardRequest;
import com.jd.live.agent.plugin.router.springcloud.v3.response.FeignClusterResponse;
import com.jd.live.agent.plugin.router.springcloud.v3.util.CloudUtils;
import feign.Request;

import java.net.URI;

import static com.jd.live.agent.core.util.ExceptionUtils.IO_EXCEPTION_CONVERTER;
import static com.jd.live.agent.governance.request.Request.KEY_CLOUD_REQUEST;
import static com.jd.live.agent.plugin.router.springcloud.v3.request.FeignOutboundRequest.createRequest;

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
        // do not static import CloudUtils to avoid class loading issue.
        if (CloudUtils.isCloudEnabled()) {
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
                mc.skipWithThrowable(FeignThrower.INSTANCE.createException(e, fr));
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
        if (!registry.prepare(service)) {
            return;
        }
        Request req = (Request) request;
        // invoke
        FeignClientClusterRequest fr = new FeignClientClusterRequest(req, service, uri, registry, endpoint ->
                mc.setArgument(0, createRequest(uri, req, endpoint)).invokeOrigin(IO_EXCEPTION_CONVERTER));
        try {
            FeignClusterResponse response = FeignClientCluster.INSTANCE.request(new HttpOutboundInvocation<>(fr, context));
            mc.skipWith(response);
        } catch (Throwable e) {
            mc.skipWithThrowable(FeignThrower.INSTANCE.createException(e, fr));
        }
    }
}
