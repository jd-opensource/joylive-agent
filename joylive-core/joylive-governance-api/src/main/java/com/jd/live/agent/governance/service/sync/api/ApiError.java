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

import com.jd.live.agent.core.util.http.HttpState;
import com.jd.live.agent.core.util.http.HttpStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ApiError implements HttpState {

    private int code;

    private String status;

    private String message;

    public ApiError(HttpStatus status) {
        this.code = status.value();
        this.status = status.name();
        this.message = status.getReasonPhrase();
    }

    public ApiError(HttpStatus status, String message) {
        this.code = status.value();
        this.status = status.name();
        this.message = message == null ? status.getReasonPhrase() : message;
    }

}
