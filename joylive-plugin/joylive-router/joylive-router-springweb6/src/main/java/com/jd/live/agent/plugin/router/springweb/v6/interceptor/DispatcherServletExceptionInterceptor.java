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
import com.jd.live.agent.core.plugin.definition.InterceptorAdaptor;
import com.jd.live.agent.governance.config.ServiceConfig;
import com.jd.live.agent.plugin.router.springweb.v6.util.CloudUtils;
import jakarta.servlet.http.HttpServletResponse;

import static com.jd.live.agent.governance.util.ResponseUtils.labelHeaders;

/**
 * Interceptor that captures exceptions from DispatcherServlet and carries them in response headers.
 *
 * @author Axkea
 */
public class DispatcherServletExceptionInterceptor extends InterceptorAdaptor {

    private final ServiceConfig config;

    public DispatcherServletExceptionInterceptor(ServiceConfig config) {
        this.config = config;
    }

    @Override
    public void onEnter(ExecutableContext ctx) {
        if (config.isResponseException()) {
            // org.springframework.web.servlet.DispatcherServlet.processHandlerException
            HttpServletResponse response = ctx.getArgument(1);
            Exception ex = ctx.getArgument(3);
            labelHeaders(ex, this::getErrorMessage, response::setHeader);
        }
    }

    private String getErrorMessage(Throwable ex) {
        return CloudUtils.getErrorMessage(ex);
    }

}
