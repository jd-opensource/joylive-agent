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
import com.jd.live.agent.core.Constants;
import com.jd.live.agent.core.plugin.definition.InterceptorAdaptor;
import org.apache.catalina.connector.ResponseFacade;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * @author Axkea
 */
public class ExceptionCarryingInterceptor extends InterceptorAdaptor {

    //The default buffer size of tomcat's response header is 1024 * 8
    private static final int DEFAULT_HEADER_SIZE_LIMIT = 1024 * 2;

    @Override
    public void onEnter(ExecutableContext ctx) {
        Object[] arguments = ctx.getArguments();
        ResponseFacade response = (ResponseFacade) arguments[1];
        Exception ex = (Exception) arguments[3];
        String exceptionNames = serializeExceptionNames(ex);
        String message = ex.getMessage();
        if (exceptionNames != null && !exceptionNames.isEmpty()) {
            response.setHeader(Constants.EXCEPTION_NAMES_LABEL, exceptionNames);
        }
        if (message != null && !message.isEmpty()) {
            String encodeMessage = Base64.getEncoder().encodeToString(message.getBytes(StandardCharsets.UTF_8));
            response.setHeader(Constants.EXCEPTION_MESSAGE_LABEL, encodeMessage);
        }
    }

    private String serializeExceptionNames(Throwable t) {
        if (t == null) {
            return null;
        }
        StringBuilder builder = new StringBuilder();
        Class<?> clazz = t.getClass();
        builder.append(clazz.getName());
        while (clazz.getSuperclass() != null && clazz.getSuperclass() != Object.class) {
            if (builder.length() * 2 >= DEFAULT_HEADER_SIZE_LIMIT) {
                break;
            }
            builder.append(Constants.EXCEPTION_NAMES_SEPARATOR);
            builder.append(clazz.getSuperclass().getName());
            clazz = clazz.getSuperclass();
        }
        return builder.toString();
    }

}
