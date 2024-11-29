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

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Axkea
 */
public class ExceptionCarryingInterceptor extends InterceptorAdaptor {

    //The default buffer size of tomcat's response header is 1024 * 8
    private static final int DEFAULT_HEADER_SIZE_LIMIT = 1024 * 2;

    private static final Set<String> exclude = new HashSet<>();

    static {
        exclude.add("java.util.concurrent.ExecutionException");
        exclude.add("java.lang.reflect.InvocationTargetException");
    }

    @Override
    public void onEnter(ExecutableContext ctx) {
        Object[] arguments = ctx.getArguments();
        ResponseFacade response = (ResponseFacade) arguments[1];
        Exception ex = (Exception) arguments[3];
        String exceptionNames = parseExceptionNames(ex);
        String message = ex.getMessage();
        if (exceptionNames != null && !exceptionNames.isEmpty()) {
            response.setHeader(Constants.EXCEPTION_NAMES_LABEL, exceptionNames);
        }
        if (message != null && !message.isEmpty()) {
            String encodeMessage = null;
            try {
                encodeMessage = URLEncoder.encode(message, Constants.EXCEPTION_NAMES_CODEC_CHARSET_NAME);
            } catch (UnsupportedEncodingException e) {
                return;
            }
            response.setHeader(Constants.EXCEPTION_MESSAGE_LABEL, encodeMessage);
        }
    }

    private String parseExceptionNames(Throwable t) {
        if (t == null) {
            return null;
        }
        Set<String> exceptionNamesSet = new HashSet<>();
        int size = 0;
        boolean isBreak = false;
        while (t != null) {
            Class<?> clazz = t.getClass();
            String clazzName = clazz.getName();
            exceptionNamesSet.add(clazzName);
            size += clazzName.getBytes().length;
            if (!exclude.contains(clazzName)) {
                while (clazz.getSuperclass() != null && clazz.getSuperclass() != Object.class) {
                    if (size >= DEFAULT_HEADER_SIZE_LIMIT) {
                        isBreak = true;
                        break;
                    }
                    String superClazzName = clazz.getSuperclass().getName();
                    exceptionNamesSet.add(superClazzName);
                    size += superClazzName.getBytes().length;
                    clazz = clazz.getSuperclass();
                }
            }
            if (size >= DEFAULT_HEADER_SIZE_LIMIT || isBreak) {
                break;
            }
            t = t.getCause();
        }

        return String.join(Constants.EXCEPTION_NAMES_SEPARATOR, exceptionNamesSet);
    }

}
