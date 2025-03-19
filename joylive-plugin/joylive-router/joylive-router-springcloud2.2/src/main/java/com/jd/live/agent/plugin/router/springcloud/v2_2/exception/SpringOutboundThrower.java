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
package com.jd.live.agent.plugin.router.springcloud.v2_2.exception;

import com.jd.live.agent.bootstrap.exception.FaultException;
import com.jd.live.agent.bootstrap.exception.LiveException;
import com.jd.live.agent.bootstrap.exception.RejectException;
import com.jd.live.agent.bootstrap.exception.RejectException.RejectCircuitBreakException;
import com.jd.live.agent.bootstrap.exception.RejectException.RejectNoProviderException;
import com.jd.live.agent.bootstrap.exception.RejectException.RejectUnreadyException;
import com.jd.live.agent.governance.exception.RetryException.RetryExhaustedException;
import com.jd.live.agent.governance.exception.RetryException.RetryTimeoutException;
import com.jd.live.agent.governance.instance.Endpoint;
import com.jd.live.agent.governance.invoke.OutboundInvocation;
import com.jd.live.agent.governance.invoke.exception.AbstractOutboundThrower;
import com.jd.live.agent.governance.request.HttpRequest.HttpOutboundRequest;
import org.springframework.core.NestedRuntimeException;
import org.springframework.http.HttpStatus;

/**
 * A concrete implementation of the OutboundThrower interface for Spring Cloud 3.x
 *
 * @see AbstractOutboundThrower
 */
public class SpringOutboundThrower<T extends Throwable, R extends HttpOutboundRequest>
        extends AbstractOutboundThrower<R, Endpoint> implements ThrowerFactory<T, R> {

    private final ThrowerFactory<T, R> factory;

    public SpringOutboundThrower(ThrowerFactory<T, R> factory) {
        this.factory = factory;
    }

    @Override
    protected T createUnReadyException(RejectUnreadyException exception, R request) {
        String message = exception.getMessage() == null ? "The cluster is not ready. " : exception.getMessage();
        return createException(request, HttpStatus.SERVICE_UNAVAILABLE, message);
    }

    @Override
    protected T createLiveException(LiveException exception, R request, Endpoint endpoint) {
        return createException(request, HttpStatus.INTERNAL_SERVER_ERROR, exception.getMessage(), exception);
    }

    @Override
    protected T createFaultException(FaultException exception, R request) {
        HttpStatus status = HttpStatus.resolve(exception.getCode());
        status = status == null ? HttpStatus.SERVICE_UNAVAILABLE : status;
        return createException(request, status, exception.getMessage(), exception);
    }

    @Override
    protected T createCircuitBreakException(RejectCircuitBreakException exception, R request) {
        return createException(request, HttpStatus.SERVICE_UNAVAILABLE, exception.getMessage(), exception);
    }

    @Override
    protected T createNoProviderException(RejectNoProviderException exception, R request) {
        return createException(request, HttpStatus.SERVICE_UNAVAILABLE,
                "LoadBalancer does not contain an instance for the service " + request.getService());
    }

    @Override
    protected T createRejectException(RejectException exception, R request) {
        return createException(request, HttpStatus.FORBIDDEN, exception.getMessage());
    }

    @Override
    protected T createRetryExhaustedException(RetryExhaustedException exception, OutboundInvocation<R> invocation) {
        return createException(invocation.getRequest(), HttpStatus.SERVICE_UNAVAILABLE, exception.getMessage());
    }

    @Override
    protected T createRetryTimeoutException(RetryTimeoutException exception, OutboundInvocation<R> invocation) {
        return createException(invocation.getRequest(), HttpStatus.SERVICE_UNAVAILABLE, exception.getMessage());
    }

    /**
     * Creates an {@link NestedRuntimeException} using the provided status, message, and headers map.
     *
     * @param status  the HTTP status code of the error
     * @param message the error message
     * @return an {@link NestedRuntimeException} instance with the specified details
     */
    protected T createException(R request, HttpStatus status, String message) {
        return createException(request, status, message, null);
    }

    @Override
    public T createException(R request, HttpStatus status, String message, Throwable throwable) {
        return factory.createException(request, status, message, throwable);
    }
}
