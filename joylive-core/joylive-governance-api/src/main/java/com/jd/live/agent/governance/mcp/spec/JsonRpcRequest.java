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

import lombok.*;

/**
 * JSON-RPC 2.0 Request Object
 * <p>
 * A rpc call is represented by sending a Request object to a Server.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class JsonRpcRequest implements JsonRpcMessage {

    public static final String JSON_PATH_ID = "$.id";

    private static final long serialVersionUID = 1L;
    /**
     * The JSON-RPC version (must be "2.0")
     */
    private String jsonrpc = JSON_RPC_VERSION;

    /**
     * The name of the method to be invoked
     */
    private String method;

    /**
     * A unique identifier for the request
     */
    private Object id;

    /**
     * Parameters for the method call
     */
    private Object params;

    public JsonRpcRequest(String method, Object id, Object params) {
        this(JSON_RPC_VERSION, method, id, params);
    }

    public JsonRpcRequest(String method, Object params) {
        this(JSON_RPC_VERSION, method, null, params);
    }

    /**
     * Check if this is a notification (request without id)
     *
     * @return true if this is a notification
     */
    public boolean notification() {
        return id == null;
    }

    public boolean validate() {
        return JSON_RPC_VERSION.equals(jsonrpc) && method != null && !method.isEmpty();
    }

}