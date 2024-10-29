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
package com.jd.live.agent.governance.invoke.exception;

import com.jd.live.agent.governance.request.ServiceRequest.InboundRequest;

/**
 * An interface for creating exceptions based on the given Throwable object and request or invocation.
 *
 * @param <R> The type of inbound request.
 */
public interface InboundThrower<R extends InboundRequest> {

    /**
     * Creates and returns an exception based on the given Throwable object and request.
     *
     * @param throwable The Throwable object that caused the exception.
     * @param request   The request that triggered the exception.
     * @return An exception instance.
     */
    Throwable createException(Throwable throwable, R request);
}
