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
package com.jd.live.agent.demo.multilive.vo;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Response<T> {

    public static final String SUCCESS = "success";

    private String requestId;

    private String status;

    private Result<T> result;

    public Response() {
    }

    public Response(String requestId, String status, Result<T> result) {
        this.requestId = requestId;
        this.status = status;
        this.result = result;
    }

    @Getter
    @Setter
    public static class Result<T> {

        private int code;

        private String message;

        private T data;

        public Result() {
        }

        public Result(int code, String message, T data) {
            this.code = code;
            this.message = message;
            this.data = data;
        }
    }

}
