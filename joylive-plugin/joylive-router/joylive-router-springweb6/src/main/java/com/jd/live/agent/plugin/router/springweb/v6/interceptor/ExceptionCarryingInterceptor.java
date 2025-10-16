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
import com.jd.live.agent.core.util.type.ClassUtils;
import com.jd.live.agent.governance.config.ServiceConfig;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.servlet.DispatcherServlet;

import static com.jd.live.agent.governance.util.ResponseUtils.labelHeaders;

/**
 * Interceptor that captures exceptions from DispatcherServlet and carries them in response headers.
 *
 * @author Axkea
 */
public class ExceptionCarryingInterceptor extends InterceptorAdaptor {

    private final ServiceConfig config;

    public ExceptionCarryingInterceptor(ServiceConfig config) {
        this.config = config;
    }

    @Override
    public void onEnter(ExecutableContext ctx) {
        // org.springframework.web.servlet.DispatcherServlet.processHandlerException
        if (config.isResponseException()) {
            HttpServletResponse response = ctx.getArgument(1);
            Exception ex = ctx.getArgument(3);
            labelHeaders(ex, Accessor::getErrorMessage, response::setHeader);
        }
    }

    /**
     * Helper class for accessing WebClientResponseException error messages.
     * Handles both WebFlux and non-WebFlux environments gracefully.
     */
    private static class Accessor {

        private static final String TYPE_EXCEPTION = "org.springframework.web.reactive.function.client.WebClientResponseException";

        private static final Class<?> CLASS_EXCEPTION = ClassUtils.loadClass(TYPE_EXCEPTION, DispatcherServlet.class.getClassLoader());

        /**
         * Safely extracts error message from exception, including WebClient response body when available.
         *
         * @param e the exception to process
         * @return the response body for WebClient exceptions, or regular message otherwise
         */
        public static String getErrorMessage(Throwable e) {
            // without webflux
            if (CLASS_EXCEPTION != null && CLASS_EXCEPTION.isInstance(e)) {
                WebClientResponseException error = (WebClientResponseException) e;
                return error.getResponseBodyAsString();
            }
            return e.getMessage();
        }

    }

}
