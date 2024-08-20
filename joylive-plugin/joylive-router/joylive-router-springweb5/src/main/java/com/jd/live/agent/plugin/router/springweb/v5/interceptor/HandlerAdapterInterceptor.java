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
package com.jd.live.agent.plugin.router.springweb.v5.interceptor;

import com.jd.live.agent.bootstrap.bytekit.context.ExecutableContext;
import com.jd.live.agent.bootstrap.bytekit.context.MethodContext;
import com.jd.live.agent.bootstrap.exception.RejectException;
import com.jd.live.agent.bootstrap.exception.RejectException.RejectEscapeException;
import com.jd.live.agent.bootstrap.exception.RejectException.RejectNoProviderException;
import com.jd.live.agent.bootstrap.logger.Logger;
import com.jd.live.agent.bootstrap.logger.LoggerFactory;
import com.jd.live.agent.core.plugin.definition.InterceptorAdaptor;
import com.jd.live.agent.governance.config.ServiceConfig;
import com.jd.live.agent.governance.invoke.InboundInvocation.HttpInboundInvocation;
import com.jd.live.agent.governance.invoke.InvocationContext;
import com.jd.live.agent.plugin.router.springweb.v5.request.ServletInboundRequest;
import org.springframework.http.HttpStatus;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * HandlerAdapterInterceptor
 */
public class HandlerAdapterInterceptor extends InterceptorAdaptor {

    private static final Logger logger = LoggerFactory.getLogger(HandlerAdapterInterceptor.class);

    private final InvocationContext context;

    public HandlerAdapterInterceptor(InvocationContext context) {
        this.context = context;
    }

    @Override
    public void onEnter(ExecutableContext ctx) {
        ServiceConfig config =  context.getGovernanceConfig().getServiceConfig();
        MethodContext mc = (MethodContext) ctx;
        Object[] arguments = ctx.getArguments();
        HttpServletResponse response = (HttpServletResponse) arguments[1];
        ServletInboundRequest request = new ServletInboundRequest((HttpServletRequest) arguments[0], arguments[2], config::isSystem);
        if (!request.isSystem()) {
            try {
                context.inbound(new HttpInboundInvocation<>(request, context));
            } catch (RejectEscapeException e) {
                error(e, HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE, response);
                mc.setSkip(true);
            } catch (RejectNoProviderException e) {
                error(e, HttpStatus.SERVICE_UNAVAILABLE, response);
                mc.setSkip(true);
            } catch (RejectException e) {
                error(e, HttpStatus.FORBIDDEN, response);
                mc.setSkip(true);
            }
        }
    }

    /**
     * Handles an error by setting the HTTP response status and writing the error message.
     *
     * @param e the RejectException containing the error details
     * @param status the HTTP status to set in the response
     * @param response the HttpServletResponse to write the error message to
     */
    private static void error(RejectException e, HttpStatus status, HttpServletResponse response) {
        if (response != null) {
            response.setStatus(status.value());
            try {
                response.getWriter().print(e.getMessage());
            } catch (IOException err) {
                logger.error("Write unit reject response error!", err);
            }
        }
    }
}
