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

import lombok.Getter;

import java.util.Set;

@Getter
public class ServiceError {

    private final String error;

    private final Throwable throwable;

    private final Set<String> exceptions;

    private final boolean serverError;

    public ServiceError(String error, boolean serverError) {
        this(error, null, null, serverError);
    }

    public ServiceError(Throwable throwable, boolean serverError) {
        this(null, throwable, null, serverError);
    }

    public ServiceError(String error, Set<String> exceptions, boolean serverError) {
        this(error, null, exceptions, serverError);
    }

    public ServiceError(String error, Throwable throwable, Set<String> exceptions, boolean serverError) {
        this.error = error;
        this.throwable = throwable;
        this.exceptions = exceptions;
        this.serverError = serverError;
    }

    public boolean hasException() {
        // ignore exception names
        return throwable != null;
    }
}
