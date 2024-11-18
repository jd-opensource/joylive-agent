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

import com.jd.live.agent.core.parser.json.JsonAlias;
import com.jd.live.agent.core.util.http.HttpResponse;
import com.jd.live.agent.core.util.http.HttpStatus;
import com.jd.live.agent.governance.service.sync.SyncResponse;
import com.jd.live.agent.governance.service.sync.SyncStatus;
import lombok.Getter;
import lombok.Setter;

import java.util.function.Function;

@Getter
@Setter
public class ApiResponse<T> {

    private String requestId;

    private ApiError error;

    @JsonAlias("data")
    private T result;

    public ApiResponse() {
    }

    public ApiResponse(String requestId, T result) {
        this.requestId = requestId;
        this.result = result;
    }

    public ApiResponse(String requestId, ApiError error) {
        this.requestId = requestId;
        this.error = error;
    }

    public HttpStatus getStatus() {
        HttpStatus status = error == null ? HttpStatus.OK : HttpStatus.resolve(error.getCode());
        return status == null ? HttpStatus.INTERNAL_SERVER_ERROR : status;
    }

    /**
     * Converts this object to a SyncResponse object with a success status.
     *
     * @return A SyncResponse object with a success status and the result of this object.
     */
    public SyncResponse<T> asSyncResponse() {
        return asSyncResponse(r -> new SyncResponse<>(SyncStatus.SUCCESS, r));
    }

    /**
     * Converts this object to a SyncResponse object using the provided function.
     *
     * @param function The function to apply to the result of this object.
     * @param <B>      The type of the result in the returned SyncResponse object.
     * @return A SyncResponse object based on the status of this object and the result of applying the provided function to the result of this object.
     */
    public <B> SyncResponse<B> asSyncResponse(Function<T, SyncResponse<B>> function) {
        switch (getStatus()) {
            case OK:
                return function.apply(result);
            case NOT_FOUND:
                return new SyncResponse<>(SyncStatus.NOT_FOUND, null);
            case NOT_MODIFIED:
                return new SyncResponse<>(SyncStatus.NOT_MODIFIED, null);
            default:
                return new SyncResponse<>(error.getMessage() != null ? error.getMessage() : "Unknown error!");
        }
    }

    /**
     * Converts an HttpResponse object to an ApiResponse object.
     *
     * @param response The HttpResponse object to convert.
     * @param <B>      The type of the result in the returned ApiResponse object.
     * @return An ApiResponse object based on the HTTP status code of the response.
     */
    public static <B> ApiResponse<B> from(HttpResponse<ApiResponse<B>> response) {
        HttpStatus status = response.getStatus();
        switch (status) {
            case OK:
                return response.getData();
            case NOT_FOUND:
                return new ApiResponse<>("", new ApiError(status, response.getMessage()));
            case NOT_MODIFIED:
                return new ApiResponse<>("", new ApiError(HttpStatus.NOT_MODIFIED));
            default:
                return new ApiResponse<>("", new ApiError(response.getStatus(), response.getMessage()));
        }
    }
}
