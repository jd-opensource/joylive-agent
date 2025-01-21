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
package com.jd.live.agent.demo.response;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@ToString
public class LiveResponse implements Serializable {

    public static final int SUCCESS = 200;

    public static final int ERROR = 500;

    public static final int NOT_FOUND = 404;

    public static final int FORBIDDEN =  403;

    private int code;

    private String message;

    private List<LiveTrace> traces;

    private Object data;

    public LiveResponse() {
    }

    public LiveResponse(Object data) {
        this.code = SUCCESS;
        this.data = data;
    }

    public LiveResponse(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public LiveResponse(int code, String message, Object data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public LiveResponse addFirst(LiveTrace trace) {
        if (traces == null) {
            traces = new ArrayList<>();
        }
        traces.add(0, trace);
        return this;
    }

    public LiveResponse addLast(LiveTrace trace) {
        if (traces == null) {
            traces = new ArrayList<>();
        }
        traces.add(trace);
        return this;
    }
}
