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
import com.jd.live.agent.core.Constants;
import com.jd.live.agent.core.plugin.definition.InterceptorAdaptor;

import javax.servlet.http.HttpServletResponse;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Axkea
 */
public class ExceptionCarryingInterceptor extends InterceptorAdaptor {

    //The default buffer size of tomcat's response header is 1024 * 8
    private static final int DEFAULT_HEADER_SIZE_LIMIT = 1024 * 2;

    private static final Set<String> exclude = new HashSet<>(Arrays.asList(
            "java.util.concurrent.ExecutionException",
            "java.lang.reflect.InvocationTargetException"
    ));

    @Override
    public void onEnter(ExecutableContext ctx) {
        // org.springframework.web.servlet.DispatcherServlet.processHandlerException
        HttpServletResponse response = ctx.getArgument(1);
        Exception ex = ctx.getArgument(3);
        String exceptionNames = parseExceptionNames(ex);
        String message = ex.getMessage();
        if (exceptionNames != null && !exceptionNames.isEmpty()) {
            response.setHeader(Constants.EXCEPTION_NAMES_LABEL, exceptionNames);
        }
        if (message != null && !message.isEmpty()) {
            try {
                String encodeMessage = URLEncoder.encode(message, StandardCharsets.UTF_8.name());
                response.setHeader(Constants.EXCEPTION_MESSAGE_LABEL, encodeMessage);
            } catch (UnsupportedEncodingException ignore) {
            }
        }
    }

    private String parseExceptionNames(Throwable t) {
        if (t == null) {
            return null;
        }
        Set<String> exceptionNames = new HashSet<>();
        int size = 0;
        boolean isBreak = false;
        while (t != null) {
            Class<?> clazz = t.getClass();
            String clazzName = clazz.getName();
            int len = clazzName.length();
            if (!exclude.contains(clazzName)) {
                if (size + len > DEFAULT_HEADER_SIZE_LIMIT) {
                    break;
                }
                if (exceptionNames.add(clazzName)) {
                    size += len;
                    clazz = clazz.getSuperclass();
                    while (clazz != null && clazz != Object.class) {
                        clazzName = clazz.getName();
                        len = clazzName.length();
                        if (size + len > DEFAULT_HEADER_SIZE_LIMIT) {
                            isBreak = true;
                            break;
                        }
                        if (exceptionNames.add(clazzName)) {
                            size += len;
                            clazz = clazz.getSuperclass();
                        } else {
                            break;
                        }
                    }
                    if (isBreak) {
                        break;
                    }
                }
            }
            t = t.getCause();
        }

        return String.join(Constants.EXCEPTION_NAMES_SEPARATOR, exceptionNames);
    }

}
