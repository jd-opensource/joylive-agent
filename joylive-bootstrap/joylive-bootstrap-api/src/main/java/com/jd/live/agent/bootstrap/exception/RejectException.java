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
package com.jd.live.agent.bootstrap.exception;

/**
 * RejectException
 *
 * @since 1.0.0
 */
public class RejectException extends LiveException {

    public RejectException() {
        super(null, null, false, false);
    }

    public RejectException(String message) {
        super(message, null, false, false);
    }

    public RejectException(String message, Throwable cause) {
        super(message, cause, false, false);
    }

    public RejectException(Throwable cause) {
        super(cause == null ? null : cause.getMessage(), cause, false, false);
    }

    /**
     * RejectUnreadyException
     */
    public static class RejectUnreadyException extends RejectException {

        public RejectUnreadyException() {
        }

        public RejectUnreadyException(String message) {
            super(message);
        }

        public RejectUnreadyException(String message, Throwable cause) {
            super(message, cause);
        }

        public RejectUnreadyException(Throwable cause) {
            super(cause);
        }

    }

    /**
     * RejectNoProviderException
     */
    public static class RejectNoProviderException extends RejectException {

        public RejectNoProviderException() {
        }

        public RejectNoProviderException(String message) {
            super(message);
        }

        public RejectNoProviderException(String message, Throwable cause) {
            super(message, cause);
        }

        public RejectNoProviderException(Throwable cause) {
            super(cause);
        }

    }

    /**
     * RejectCellException
     */
    public static class RejectCellException extends RejectException {

        public RejectCellException() {
        }

        public RejectCellException(String message) {
            super(message);
        }

        public RejectCellException(String message, Throwable cause) {
            super(message, cause);
        }

        public RejectCellException(Throwable cause) {
            super(cause);
        }

    }

    /**
     * RejectUnitException
     */
    public static class RejectUnitException extends RejectException {

        public RejectUnitException() {
        }

        public RejectUnitException(String message) {
            super(message);
        }

        public RejectUnitException(String message, Throwable cause) {
            super(message, cause);
        }

        public RejectUnitException(Throwable cause) {
            super(cause);
        }

    }

    /**
     * RejectEscapeException
     */
    public static class RejectEscapeException extends RejectUnitException {

        public RejectEscapeException() {
        }

        public RejectEscapeException(String message) {
            super(message);
        }

        public RejectEscapeException(String message, Throwable cause) {
            super(message, cause);
        }

        public RejectEscapeException(Throwable cause) {
            super(cause);
        }

    }

    /**
     * RejectPermissionException
     */
    public static class RejectPermissionException extends RejectException {

        public RejectPermissionException() {
        }

        public RejectPermissionException(String message) {
            super(message);
        }

        public RejectPermissionException(String message, Throwable cause) {
            super(message, cause);
        }

        public RejectPermissionException(Throwable cause) {
            super(cause);
        }

    }

    /**
     * RejectAuthException
     */
    public static class RejectAuthException extends RejectException {

        public RejectAuthException() {
        }

        public RejectAuthException(String message) {
            super(message);
        }

        public RejectAuthException(String message, Throwable cause) {
            super(message, cause);
        }

        public RejectAuthException(Throwable cause) {
            super(cause);
        }

    }

    /**
     * RejectLimitException
     */
    public static class RejectLimitException extends RejectException {

        public RejectLimitException() {
        }

        public RejectLimitException(String message) {
            super(message);
        }

        public RejectLimitException(String message, Throwable cause) {
            super(message, cause);
        }

        public RejectLimitException(Throwable cause) {
            super(cause);
        }

    }

    /**
     * RejectCircuitBreakException
     */
    public static class RejectCircuitBreakException extends RejectException {

        private final Object config;

        public RejectCircuitBreakException(String message) {
            super(message);
            this.config = null;
        }

        public RejectCircuitBreakException(String message, Object config) {
            super(message);
            this.config = config;
        }

        public RejectCircuitBreakException(String message, Throwable cause, Object config) {
            super(message, cause);
            this.config = config;
        }

        public RejectCircuitBreakException(Throwable cause, Object config) {
            super(cause);
            this.config = config;
        }

        @SuppressWarnings("unchecked")
        public <T> T getConfig() {
            return (T) config;
        }

        /**
         * Checks if the provided throwable or its cause is an instance of {@link RejectCircuitBreakException}.
         *
         * @param throwable the throwable to check for {@link RejectCircuitBreakException}.
         * @return the {@link RejectCircuitBreakException} if found, otherwise null.
         */
        public static RejectCircuitBreakException getCircuitBreakException(Throwable throwable) {
            if (throwable instanceof RejectCircuitBreakException) {
                return (RejectCircuitBreakException) throwable;
            } else if (throwable.getCause() instanceof RejectCircuitBreakException) {
                return (RejectCircuitBreakException) throwable.getCause();
            }
            return null;
        }
    }
}