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
package com.jd.live.agent.governance.exception;

import com.jd.live.agent.bootstrap.exception.LiveException;
import lombok.Getter;

/**
 * RetryException
 */
public class RetryException extends LiveException {

    public RetryException() {
    }

    public RetryException(String message) {
        super(message);
    }

    public RetryException(String message, Throwable cause) {
        super(message, cause);
    }

    public RetryException(Throwable cause) {
        super(cause);
    }

    public RetryException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    /**
     * Indicates that an operation has exceeded the maximum number of retry attempts without success.
     * <p>
     * This exception is thrown in scenarios where retry logic is applied to an operation, and the operation
     * continues to fail after the maximum number of retries has been reached. It serves as a clear indicator
     * that all attempts to retry the operation have been exhausted, and further action may be required to
     * handle the failure. This could involve logging the failure, alerting an administrator, or triggering
     * alternative recovery mechanisms.
     * </p>
     */
    @Getter
    public static class RetryExhaustedException extends RetryException {

        private final int attempts;

        public RetryExhaustedException(String message, int attempts) {
            super(message);
            this.attempts = attempts;
        }

        public RetryExhaustedException(String message, Throwable cause, int attempts) {
            super(message, cause);
            this.attempts = attempts;
        }
    }

    /**
     * Exception class to indicate that a retry operation has timed out.
     * <p>
     * This exception is thrown when a retryable operation does not complete within
     * the specified timeout period. The timeout duration is captured and can be
     * retrieved using the {@link #getTimeout()} method.
     * </p>
     */
    @Getter
    public static class RetryTimeoutException extends RetryException {

        private final long timeout;

        public RetryTimeoutException(String message, long timeout) {
            super(message);
            this.timeout = timeout;
        }

        public RetryTimeoutException(String message, Throwable cause, long timeout) {
            super(message, cause);
            this.timeout = timeout;
        }
    }

}
