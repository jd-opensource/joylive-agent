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

import com.jd.live.agent.core.exception.WrappedException;

/**
 * GrpcException
 *
 * @see GrpcException
 */
public abstract class GrpcException extends RuntimeException implements WrappedException {

    public GrpcException(Throwable cause) {
        super(cause.getMessage(), cause, false, false);
    }

    /**
     * GrpcClientException
     */
    public static class GrpcClientException extends GrpcException {

        public GrpcClientException(Throwable cause) {
            super(cause);
        }

    }

    /**
     * GrpcException
     *
     * @see GrpcServerException
     */
    public static class GrpcServerException extends GrpcException {

        public GrpcServerException(Throwable cause) {
            super(cause);
        }

    }
}
