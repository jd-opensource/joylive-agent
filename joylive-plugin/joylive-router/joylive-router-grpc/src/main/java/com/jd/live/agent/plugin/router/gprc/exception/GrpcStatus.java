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
package com.jd.live.agent.plugin.router.gprc.exception;

import com.jd.live.agent.bootstrap.exception.LiveException;
import com.jd.live.agent.bootstrap.exception.RejectException;
import com.jd.live.agent.bootstrap.exception.RejectException.*;
import io.grpc.Status;

/**
 * A utility class that provides methods for creating gRPC Status objects from various types of exceptions.
 */
public class GrpcStatus {

    /**
     * Creates a gRPC Status object from the given Throwable object.
     *
     * @param throwable The Throwable object to convert to a gRPC Status object.
     * @return The gRPC Status object corresponding to the given Throwable object.
     */
    public static Status createException(Throwable throwable) {
        if (throwable == null) {
            return Status.OK;
        } else if (throwable instanceof RejectUnreadyException) {
            return createUnReadyException((RejectUnreadyException) throwable);
        } else if (throwable instanceof RejectAuthException) {
            return createAuthException((RejectAuthException) throwable);
        } else if (throwable instanceof RejectPermissionException) {
            return createPermissionException((RejectPermissionException) throwable);
        } else if (throwable instanceof RejectEscapeException) {
            return createEscapeException((RejectEscapeException) throwable);
        } else if (throwable instanceof RejectLimitException) {
            return createLimitException((RejectLimitException) throwable);
        } else if (throwable instanceof RejectCircuitBreakException) {
            return createCircuitBreakException((RejectCircuitBreakException) throwable);
        } else if (throwable instanceof RejectException) {
            return createRejectException((RejectException) throwable);
        } else if (throwable instanceof LiveException) {
            return createLiveException((LiveException) throwable);
        } else {
            return createUnknownException(throwable);
        }
    }

    /**
     * Creates a gRPC Status object for a RejectUnreadyException.
     *
     * @param exception The RejectUnreadyException object to convert to a gRPC Status object.
     * @return The gRPC Status object for the given RejectUnreadyException object.
     */
    protected static Status createUnReadyException(RejectUnreadyException exception) {
        return Status.UNAVAILABLE.withDescription(exception.getMessage());
    }

    /**
     * Creates a gRPC Status object for a LiveException.
     *
     * @param exception The LiveException object to convert to a gRPC Status object.
     * @return The gRPC Status object for the given LiveException object.
     */
    public static Status createLiveException(LiveException exception) {
        return Status.INTERNAL.withDescription(exception.getMessage());
    }

    /**
     * Creates a gRPC Status object for a RejectPermissionException.
     *
     * @param exception The RejectPermissionException object to convert to a gRPC Status object.
     * @return The gRPC Status object for the given RejectPermissionException object.
     */
    protected static Status createPermissionException(RejectPermissionException exception) {
        return Status.PERMISSION_DENIED.withDescription(exception.getMessage());
    }

    /**
     * Creates a gRPC Status object for a RejectAuthException.
     *
     * @param exception The RejectAuthException object to convert to a gRPC Status object.
     * @return The gRPC Status object for the given RejectAuthException object.
     */
    protected static Status createAuthException(RejectAuthException exception) {
        return Status.UNAUTHENTICATED.withDescription(exception.getMessage());
    }

    /**
     * Creates a gRPC Status object for a RejectLimitException.
     *
     * @param exception The RejectLimitException object to convert to a gRPC Status object.
     * @return The gRPC Status object for the given RejectLimitException object.
     */
    protected static Status createLimitException(RejectLimitException exception) {
        return Status.RESOURCE_EXHAUSTED.withDescription(exception.getMessage());
    }

    /**
     * Creates a gRPC Status object for a RejectCircuitBreakException.
     *
     * @param exception The RejectCircuitBreakException object to convert to a gRPC Status object.
     * @return The gRPC Status object for the given RejectCircuitBreakException object.
     */
    protected static Status createCircuitBreakException(RejectCircuitBreakException exception) {
        return Status.RESOURCE_EXHAUSTED.withDescription(exception.getMessage());
    }

    /**
     * Creates a gRPC Status object for a RejectEscapeException.
     *
     * @param exception The RejectEscapeException object to convert to a gRPC Status object.
     * @return The gRPC Status object for the given RejectEscapeException object.
     */
    protected static Status createEscapeException(RejectEscapeException exception) {
        return Status.OUT_OF_RANGE.withDescription(exception.getMessage());
    }

    /**
     * Creates a gRPC Status object for a RejectException.
     *
     * @param exception The RejectException object to convert to a gRPC Status object.
     * @return The gRPC Status object for the given RejectException object.
     */
    protected static Status createRejectException(RejectException exception) {
        return Status.INTERNAL.withDescription(exception.getMessage());
    }

    /**
     * Creates a gRPC Status object for an unknown exception.
     *
     * @param exception The Throwable object to convert to a gRPC Status object.
     * @return The gRPC Status object for the given Throwable object.
     */
    protected static Status createUnknownException(Throwable exception) {
        return Status.INTERNAL.withDescription(exception.getMessage());
    }
}
