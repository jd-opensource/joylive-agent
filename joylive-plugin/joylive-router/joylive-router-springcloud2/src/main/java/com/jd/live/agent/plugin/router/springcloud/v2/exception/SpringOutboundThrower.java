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
package com.jd.live.agent.plugin.router.springcloud.v2.exception;

import com.jd.live.agent.bootstrap.exception.FaultException;
import com.jd.live.agent.bootstrap.exception.LiveException;
import com.jd.live.agent.bootstrap.exception.RejectException;
import com.jd.live.agent.bootstrap.exception.RejectException.RejectCircuitBreakException;
import com.jd.live.agent.bootstrap.exception.RejectException.RejectNoProviderException;
import com.jd.live.agent.bootstrap.exception.RejectException.RejectUnreadyException;
import com.jd.live.agent.governance.exception.RetryException.RetryExhaustedException;
import com.jd.live.agent.governance.exception.RetryException.RetryTimeoutException;
import com.jd.live.agent.governance.invoke.OutboundInvocation;
import com.jd.live.agent.governance.invoke.exception.AbstractOutboundThrower;
import com.jd.live.agent.governance.request.HttpRequest.HttpOutboundRequest;
import com.jd.live.agent.plugin.router.springcloud.v2.instance.SpringEndpoint;
import org.springframework.core.NestedRuntimeException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * A concrete implementation of the OutboundThrower interface for Spring Cloud 3.x
 *
 * @see AbstractOutboundThrower
 */
public class SpringOutboundThrower<R extends HttpOutboundRequest> extends AbstractOutboundThrower<R, SpringEndpoint> {

    @Override
    protected NestedRuntimeException createUnReadyException(RejectUnreadyException exception, R request) {
        String message = exception.getMessage() == null ? "The cluster is not ready. " : exception.getMessage();
        return createException(HttpStatus.SERVICE_UNAVAILABLE, message);
    }

    @Override
    protected NestedRuntimeException createLiveException(LiveException exception, R request, SpringEndpoint endpoint) {
        return createException(HttpStatus.INTERNAL_SERVER_ERROR, exception.getMessage(), exception);
    }

    @Override
    protected NestedRuntimeException createFaultException(FaultException exception, R request) {
        HttpStatus status = HttpStatus.resolve(exception.getCode());
        status = status == null ? HttpStatus.SERVICE_UNAVAILABLE : status;
        return createException(status, exception.getMessage(), exception);
    }

    @Override
    protected NestedRuntimeException createCircuitBreakException(RejectCircuitBreakException exception, R request) {
        return createException(HttpStatus.SERVICE_UNAVAILABLE, exception.getMessage(), exception);
    }

    @Override
    protected NestedRuntimeException createNoProviderException(RejectNoProviderException exception, R request) {
        return createException(HttpStatus.SERVICE_UNAVAILABLE,
                "LoadBalancer does not contain an instance for the service " + request.getService());
    }

    @Override
    protected NestedRuntimeException createRejectException(RejectException exception, R request) {
        return createException(HttpStatus.FORBIDDEN, exception.getMessage());
    }

    @Override
    protected NestedRuntimeException createRetryExhaustedException(RetryExhaustedException exception, OutboundInvocation<R> invocation) {
        return createException(HttpStatus.SERVICE_UNAVAILABLE, exception.getMessage());
    }

    @Override
    protected NestedRuntimeException createRetryTimeoutException(RetryTimeoutException exception, OutboundInvocation<R> invocation) {
        return createException(HttpStatus.SERVICE_UNAVAILABLE, exception.getMessage());
    }

    /**
     * Creates an {@link NestedRuntimeException} using the provided status, message, and headers map.
     *
     * @param status  the HTTP status code of the error
     * @param message the error message
     * @return an {@link NestedRuntimeException} instance with the specified details
     */
    public static NestedRuntimeException createException(HttpStatus status, String message) {
        return createException(status, message, null);
    }

    /**
     * Creates an {@link NestedRuntimeException} using the provided status, message, and {@link HttpHeaders}.
     *
     * @param status    the HTTP status code of the error
     * @param message   the error message
     * @param throwable the exception
     * @return an {@link NestedRuntimeException} instance with the specified details
     */
    public static NestedRuntimeException createException(HttpStatus status, String message, Throwable throwable) {
        return new ResponseStatusException(status, message, throwable);
    }
}
