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
package com.jd.live.agent.governance.service.sync.api;

import com.jd.live.agent.core.util.http.HttpStatus;
import com.jd.live.agent.governance.service.sync.SyncResponse;
import com.jd.live.agent.governance.service.sync.SyncStatus;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ApiResponse<T> {

    private String requestId;

    private ApiError error;

    private T data;

    public ApiResponse() {
    }

    public ApiResponse(String requestId, T data) {
        this.requestId = requestId;
        this.data = data;
    }

    public ApiResponse(String requestId, ApiError error) {
        this.requestId = requestId;
        this.error = error;
    }

    public HttpStatus getStatus() {
        HttpStatus status = error == null ? HttpStatus.OK : HttpStatus.resolve(error.getCode());
        return status == null ? HttpStatus.INTERNAL_SERVER_ERROR : status;
    }

    public SyncResponse<T> toSyncResponse() {
        switch (getStatus()) {
            case OK:
                return new SyncResponse<>(SyncStatus.SUCCESS, data);
            case NOT_FOUND:
                return new SyncResponse<>(SyncStatus.NOT_FOUND, data);
            case NOT_MODIFIED:
                return new SyncResponse<>(SyncStatus.NOT_MODIFIED, data);
            default:
                return new SyncResponse<>(error.getMessage());
        }
    }
}
