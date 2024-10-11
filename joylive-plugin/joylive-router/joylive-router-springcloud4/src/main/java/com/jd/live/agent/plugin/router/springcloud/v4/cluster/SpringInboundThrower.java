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
package com.jd.live.agent.plugin.router.springcloud.v4.cluster;

import com.jd.live.agent.bootstrap.exception.RejectException;
import com.jd.live.agent.bootstrap.exception.RejectException.*;
import com.jd.live.agent.governance.invoke.exception.AbstractInboundThrower;
import com.jd.live.agent.governance.request.HttpRequest.HttpInboundRequest;
import org.springframework.core.NestedRuntimeException;
import org.springframework.http.HttpStatus;

/**
 * A concrete implementation of the InboundThrower interface for Spring Cloud 4.x.
 *
 * @see AbstractInboundThrower
 */
public class SpringInboundThrower extends AbstractInboundThrower<HttpInboundRequest, NestedRuntimeException> {

    public static final SpringInboundThrower INSTANCE = new SpringInboundThrower();

    @Override
    protected boolean isNativeException(Throwable throwable) {
        return throwable instanceof NestedRuntimeException;
    }

    @Override
    protected NestedRuntimeException createUnReadyException(RejectUnreadyException exception, HttpInboundRequest request) {
        String message = exception.getMessage() == null ? "The cluster is not ready. " : exception.getMessage();
        return SpringOutboundThrower.createException(HttpStatus.SERVICE_UNAVAILABLE, message);
    }

    @Override
    protected NestedRuntimeException createUnknownException(Throwable throwable, HttpInboundRequest request) {
        return SpringOutboundThrower.createException(HttpStatus.INTERNAL_SERVER_ERROR, throwable.getMessage(), throwable);

    }

    @Override
    protected NestedRuntimeException createPermissionException(RejectPermissionException exception, HttpInboundRequest request) {
        return SpringOutboundThrower.createException(HttpStatus.UNAUTHORIZED, exception.getMessage());
    }

    @Override
    protected NestedRuntimeException createAuthException(RejectAuthException exception, HttpInboundRequest request) {
        return SpringOutboundThrower.createException(HttpStatus.UNAUTHORIZED, exception.getMessage());
    }

    @Override
    protected NestedRuntimeException createLimitException(RejectLimitException exception, HttpInboundRequest request) {
        return SpringOutboundThrower.createException(HttpStatus.TOO_MANY_REQUESTS, exception.getMessage());
    }

    @Override
    protected NestedRuntimeException createCircuitBreakException(RejectCircuitBreakException exception, HttpInboundRequest request) {
        return SpringOutboundThrower.createException(HttpStatus.SERVICE_UNAVAILABLE, exception.getMessage(), exception);
    }

    @Override
    protected NestedRuntimeException createEscapeException(RejectEscapeException exception, HttpInboundRequest request) {
        return SpringOutboundThrower.createException(HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE, exception.getMessage(), exception);
    }

    @Override
    protected NestedRuntimeException createRejectException(RejectException exception, HttpInboundRequest request) {
        return SpringOutboundThrower.createException(HttpStatus.FORBIDDEN, exception.getMessage());
    }
}
