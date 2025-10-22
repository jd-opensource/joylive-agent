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
package com.jd.live.agent.governance.jsonrpc;

import java.io.Serializable;

/**
 * JSON-RPC 2.0 Request Object
 * <p>
 * A rpc call is represented by sending a Request object to a Server.
 */
public class JsonRpcRequest implements Serializable {

    public static final String JSON_PATH_ID = "$.id";

    private static final long serialVersionUID = 1L;

    /**
     * A String specifying the version of the JSON-RPC protocol. MUST be exactly "2.0".
     */
    private String jsonrpc = "2.0";

    /**
     * A String containing the name of the method to be invoked.
     */
    private String method;

    /**
     * A Structured value that holds the parameter values to be used during the invocation of the method.
     * This member MAY be omitted.
     */
    private Object params;

    /**
     * An identifier established by the Client that MUST contain a String, Number, or NULL value if included.
     * If it is not included it is assumed to be a notification.
     */
    private Object id;

    public JsonRpcRequest() {
    }

    public JsonRpcRequest(String method, Object params, Object id) {
        this.method = method;
        this.params = params;
        this.id = id;
    }

    public JsonRpcRequest(String method, Object params) {
        this.method = method;
        this.params = params;
    }

    public String getJsonrpc() {
        return jsonrpc;
    }

    public void setJsonrpc(String jsonrpc) {
        this.jsonrpc = jsonrpc;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public Object getParams() {
        return params;
    }

    public void setParams(Object params) {
        this.params = params;
    }

    public Object getId() {
        return id;
    }

    public void setId(Object id) {
        this.id = id;
    }

    /**
     * Check if this is a notification (request without id)
     *
     * @return true if this is a notification
     */
    public boolean notification() {
        return id == null;
    }

    @Override
    public String toString() {
        return "JsonRpcRequest{" +
                "jsonrpc='" + jsonrpc + '\'' +
                ", method='" + method + '\'' +
                ", params=" + params +
                ", id=" + id +
                '}';
    }
}