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
    }

    public RejectException(String message) {
        super(message);
    }

    public RejectException(String message, Throwable cause) {
        super(message, cause);
    }

    public RejectException(Throwable cause) {
        super(cause);
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
}