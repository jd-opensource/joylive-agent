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
package com.jd.live.agent.plugin.router.springweb.v6.util;

import com.jd.live.agent.bootstrap.util.type.FieldAccessor;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import static com.jd.live.agent.bootstrap.util.type.FieldAccessorFactory.getAccessor;
import static com.jd.live.agent.core.util.type.ClassUtils.loadClass;

/**
 * Utility class for detecting Spring Cloud environment and load balancer configuration.
 */
public class CloudUtils {

    private static final String TYPE_WEB_CLIENT_RESPONSE_EXCEPTION = "org.springframework.web.reactive.function.client.WebClientResponseException";

    private static final Class<?> CLASS_WEB_CLIENT_RESPONSE_EXCEPTION = loadClass(TYPE_WEB_CLIENT_RESPONSE_EXCEPTION, HttpHeaders.class.getClassLoader());

    private static final String TYPE_HANDLER_METHOD = "org.springframework.web.method.HandlerMethod";

    private static final Class<?> CLASS_HANDLER_METHOD = loadClass(TYPE_HANDLER_METHOD, HttpHeaders.class.getClassLoader());

    private static final FieldAccessor ACCESSOR_HANDLER = getAccessor(CLASS_HANDLER_METHOD, "bean");

    /**
     * Creates writable copy of HTTP headers.
     *
     * @param headers source headers
     * @return writable headers instance
     */
    public static HttpHeaders writable(HttpHeaders headers) {
        return HttpHeaders.writableHttpHeaders(headers);
    }

    public static Object getHandler(Object handlerMethod) {
        return handlerMethod != null && CLASS_HANDLER_METHOD.isInstance(handlerMethod) ? ACCESSOR_HANDLER.get(handlerMethod) : null;
    }

    /**
     * Safely extracts error message from exception, including WebClient response body when available.
     *
     * @param e the exception to process
     * @return the response body for WebClient exceptions, or regular message otherwise
     */
    public static String getErrorMessage(Throwable e) {
        // without webflux
        if (CLASS_WEB_CLIENT_RESPONSE_EXCEPTION != null && CLASS_WEB_CLIENT_RESPONSE_EXCEPTION.isInstance(e)) {
            WebClientResponseException webError = (WebClientResponseException) e;
            return webError.getResponseBodyAsString();
        }
        return e.getMessage();
    }
}
