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

@Getter
public class ServiceError {

    private final String error;

    private final Throwable throwable;

    private final boolean serverError;

    public ServiceError(String error, boolean serverError) {
        this(error, null, serverError);
    }

    public ServiceError(Throwable throwable, boolean serverError) {
        this(null, throwable, serverError);
    }

    public ServiceError(String error, Throwable throwable, boolean serverError) {
        this.error = error == null && throwable != null ? throwable.getMessage() : error;
        this.throwable = throwable;
        this.serverError = serverError;
    }
}
