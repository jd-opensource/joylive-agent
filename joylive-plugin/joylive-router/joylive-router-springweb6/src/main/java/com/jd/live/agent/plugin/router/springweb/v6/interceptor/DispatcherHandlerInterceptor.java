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
package com.jd.live.agent.plugin.router.springweb.v6.interceptor;

import com.jd.live.agent.bootstrap.bytekit.context.ExecutableContext;
import com.jd.live.agent.bootstrap.bytekit.context.MethodContext;
import com.jd.live.agent.bootstrap.exception.RejectException;
import com.jd.live.agent.bootstrap.exception.RejectException.RejectEscapeException;
import com.jd.live.agent.bootstrap.exception.RejectException.RejectNoProviderException;
import com.jd.live.agent.core.plugin.definition.InterceptorAdaptor;
import com.jd.live.agent.governance.config.ServiceConfig;
import com.jd.live.agent.governance.invoke.InboundInvocation;
import com.jd.live.agent.governance.invoke.InboundInvocation.GatewayInboundInvocation;
import com.jd.live.agent.governance.invoke.InboundInvocation.HttpInboundInvocation;
import com.jd.live.agent.governance.invoke.InvocationContext;
import com.jd.live.agent.plugin.router.springweb.v6.request.ReactiveInboundRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * DispatcherHandlerInterceptor
 */
public class DispatcherHandlerInterceptor extends InterceptorAdaptor {

    private final InvocationContext context;

    public DispatcherHandlerInterceptor(InvocationContext context) {
        this.context = context;
    }

    @Override
    public void onEnter(ExecutableContext ctx) {
        ServiceConfig config = context.getGovernanceConfig().getServiceConfig();
        MethodContext mc = (MethodContext) ctx;
        ServerWebExchange exchange = (ServerWebExchange) mc.getArguments()[0];
        Object handler = mc.getArguments()[1];
        ReactiveInboundRequest request = new ReactiveInboundRequest(exchange.getRequest(), handler, config::isSystem);
        if (!request.isSystem()) {
            try {
                InboundInvocation<ReactiveInboundRequest> invocation = context.getApplication().getService().isGateway()
                        ? new GatewayInboundInvocation<>(request, context)
                        : new HttpInboundInvocation<>(request, context);
                context.inbound(invocation);
            } catch (RejectEscapeException e) {
                mc.setResult(Mono.error(new ResponseStatusException(HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE, e.getMessage(), e)));
                mc.setSkip(true);
            } catch (RejectNoProviderException e) {
                mc.setResult(Mono.error(new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, e.getMessage(), e)));
                mc.setSkip(true);
            } catch (RejectException.RejectAuthException e) {
                mc.setResult(Mono.error(new ResponseStatusException(HttpStatus.UNAUTHORIZED, e.getMessage(), e)));
                mc.setSkip(true);
            } catch (RejectException e) {
                mc.setResult(Mono.error(new ResponseStatusException(HttpStatus.FORBIDDEN, e.getMessage(), e)));
                mc.setSkip(true);
            }
        }
    }
}
