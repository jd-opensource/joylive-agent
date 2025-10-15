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
package com.jd.live.agent.plugin.router.springweb.v5.exception;

import com.jd.live.agent.bootstrap.exception.LiveException;
import com.jd.live.agent.bootstrap.exception.RejectException;
import com.jd.live.agent.bootstrap.exception.RejectException.*;
import com.jd.live.agent.governance.invoke.exception.AbstractInboundThrower;
import com.jd.live.agent.governance.request.HttpRequest.HttpInboundRequest;
import org.springframework.core.NestedRuntimeException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;

import java.lang.reflect.Constructor;

import static com.jd.live.agent.core.util.type.ClassUtils.getDeclaredConstructor;
import static com.jd.live.agent.core.util.type.ClassUtils.loadClass;

/**
 * A concrete implementation of the InboundThrower interface for Spring Cloud 3.x
 *
 * @see AbstractInboundThrower
 */
public class SpringInboundThrower extends AbstractInboundThrower<HttpInboundRequest> {

    private static final String TYPE_RESPONSE_STATUS_EXCEPTION = "org.springframework.web.server.ResponseStatusException";
    private static final String TYPE_SERVLET_EXCEPTION = "javax.servlet.ServletException";
    // spring web 5+
    private static final Class<?> CLASS_RESPONSE_STATUS_EXCEPTION = loadClass(TYPE_RESPONSE_STATUS_EXCEPTION, HttpStatus.class.getClassLoader());
    // spring web 4.x
    private static final Class<?> CLASS_SERVLET_EXCEPTION = loadClass(TYPE_SERVLET_EXCEPTION, HttpStatus.class.getClassLoader());
    private static final Constructor<?> CONSTRUCTOR_SERVLET_EXCEPTION = getDeclaredConstructor(CLASS_SERVLET_EXCEPTION, new Class[]{String.class, Throwable.class});
    private static final Constructor<?> CONSTRUCTOR_RESPONSE_STATUS_EXCEPTION = getDeclaredConstructor(CLASS_RESPONSE_STATUS_EXCEPTION, new Class[]{HttpStatus.class, String.class, Throwable.class});

    public static final SpringInboundThrower THROWER = new SpringInboundThrower();

    @Override
    protected Throwable createUnReadyException(RejectUnreadyException exception, HttpInboundRequest request) {
        String message = exception.getMessage() == null ? "The cluster is not ready. " : exception.getMessage();
        return createException(HttpStatus.SERVICE_UNAVAILABLE, message);
    }

    @Override
    protected Throwable createLiveException(LiveException exception, HttpInboundRequest request) {
        return createException(HttpStatus.INTERNAL_SERVER_ERROR, exception.getMessage(), exception);
    }

    @Override
    protected Throwable createPermissionException(RejectPermissionException exception, HttpInboundRequest request) {
        return createException(HttpStatus.UNAUTHORIZED, exception.getMessage());
    }

    @Override
    protected Throwable createAuthException(RejectAuthException exception, HttpInboundRequest request) {
        return createException(HttpStatus.UNAUTHORIZED, exception.getMessage());
    }

    @Override
    protected Throwable createLimitException(RejectLimitException exception, HttpInboundRequest request) {
        return createException(HttpStatus.TOO_MANY_REQUESTS, exception.getMessage());
    }

    @Override
    protected Throwable createCircuitBreakException(RejectCircuitBreakException exception, HttpInboundRequest request) {
        return createException(HttpStatus.SERVICE_UNAVAILABLE, exception.getMessage(), exception);
    }

    @Override
    protected Throwable createEscapeException(RejectEscapeException exception, HttpInboundRequest request) {
        return createException(HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE, exception.getMessage(), exception);
    }

    @Override
    protected Throwable createRejectException(RejectException exception, HttpInboundRequest request) {
        return createException(HttpStatus.FORBIDDEN, exception.getMessage());
    }

    /**
     * Creates an {@link Throwable} using the provided status, message, and headers map.
     *
     * @param status  the HTTP status code of the error
     * @param message the error message
     * @return an {@link NestedRuntimeException} instance with the specified details
     */
    public static Throwable createException(HttpStatus status, String message) {
        return createException(status, message, null);
    }

    /**
     * Creates an {@link Throwable} using the provided status, message, and {@link HttpHeaders}.
     *
     * @param status    the HTTP status code of the error
     * @param message   the error message
     * @param throwable the exception
     * @return an {@link Throwable} instance with the specified details
     */
    public static Throwable createException(HttpStatus status, String message, Throwable throwable) {
        try {
            if (CONSTRUCTOR_RESPONSE_STATUS_EXCEPTION != null) {
                // spring web 5+
                return (Throwable) CONSTRUCTOR_RESPONSE_STATUS_EXCEPTION.newInstance(status, message, throwable);
            } else if (CLASS_SERVLET_EXCEPTION != null) {
                // spring web 4.x
                return (Throwable) CONSTRUCTOR_SERVLET_EXCEPTION.newInstance(message, throwable);
            } else {
                return new RuntimeException(message, throwable);
            }
        } catch (Throwable e) {
            return new RuntimeException(message, throwable);
        }
    }
}
