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
package com.jd.live.agent.governance.service.sync;

import lombok.Getter;

@Getter
public class SyncResponse<T> {

    private final SyncStatus status;

    private final T data;

    private final String error;

    private final Throwable throwable;

    public SyncResponse(SyncStatus status, T data) {
        this(status, data, null, null);
    }

    public SyncResponse(String error) {
        this(SyncStatus.ERROR, null, error, null);
    }

    public SyncResponse(Throwable throwable) {
        this(SyncStatus.ERROR, null, throwable == null ? null : throwable.getMessage(), throwable);
    }

    public SyncResponse(SyncStatus status, T data, String error, Throwable throwable) {
        this.status = status;
        this.data = data;
        this.error = error;
        this.throwable = throwable;
    }
}
