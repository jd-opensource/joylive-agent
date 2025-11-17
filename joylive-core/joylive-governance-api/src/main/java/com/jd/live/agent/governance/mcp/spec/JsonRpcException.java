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
package com.jd.live.agent.governance.mcp.spec;

public class JsonRpcException extends RuntimeException {

    public JsonRpcException(String message, int code) {
        this(message, null, code);
    }

    /**
     * A Number that indicates the error type that occurred.
     * This MUST be an integer.
     */
    private int code;

    public JsonRpcException(String message, Throwable cause, int code) {
        super(message, cause, false, false);
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    public static class NotEnoughParameter extends JsonRpcException {

        public NotEnoughParameter() {
            super("Not enough parameters", ErrorCodes.INVALID_PARAMS);
        }

    }
}
