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
import com.jd.live.agent.bootstrap.exception.RejectException;
import com.jd.live.agent.bootstrap.logger.Logger;
import com.jd.live.agent.bootstrap.logger.LoggerFactory;
import com.jd.live.agent.governance.interceptor.AbstractInterceptor.AbstractHttpInboundInterceptor;
import com.jd.live.agent.governance.invoke.InvocationContext;
import com.jd.live.agent.governance.invoke.filter.InboundFilter;
import com.jd.live.agent.plugin.router.springcloud.v3.request.ServletInboundRequest;
import org.springframework.http.HttpStatus;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

/**
 * DispatcherServletInterceptor
 *
 * @author Zhiguo.Chen
 * @since 1.0.0
 */
public class DispatcherServletInterceptor extends AbstractHttpInboundInterceptor<ServletInboundRequest> {

    private static final Logger logger = LoggerFactory.getLogger(DispatcherServletInterceptor.class);

    public DispatcherServletInterceptor(InvocationContext context, List<InboundFilter> filters) {
        super(context, filters);
    }

    /**
     * Enhanced logic before method execution
     *
     * <p>
     * see org.springframework.web.servlet.DispatcherServlet#doService(HttpServletRequest, HttpServletResponse)
     *
     * @param ctx ExecutableContext
     */
    @Override
    public void onEnter(ExecutableContext ctx) {
        MethodContext mc = (MethodContext) ctx;
        Object[] arguments = ctx.getArguments();
        HttpServletResponse response = (HttpServletResponse) arguments[1];
        try {
            process(new ServletInboundRequest((HttpServletRequest) arguments[0]));
        } catch (RejectException e) {
            mc.setSkip(true);
            if (response != null) {
                response.setStatus(HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE.value());
                try {
                    response.getWriter().print(e.getMessage());
                } catch (IOException err) {
                    logger.error("Write unit reject response error!", err);
                }
            }
        }
    }
}
