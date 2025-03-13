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

import org.springframework.core.NestedRuntimeException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;

/**
 * A factory interface for creating instances of {@link NestedRuntimeException}.
 */
public interface ThrowerFactory {

    /**
     * Creates an {@link NestedRuntimeException} using the provided status, message, and {@link HttpHeaders}.
     *
     * @param status    the HTTP status code of the error
     * @param message   the error message
     * @param throwable the exception
     * @return an {@link NestedRuntimeException} instance with the specified details
     */
    NestedRuntimeException createException(HttpStatus status, String message, Throwable throwable);

}
