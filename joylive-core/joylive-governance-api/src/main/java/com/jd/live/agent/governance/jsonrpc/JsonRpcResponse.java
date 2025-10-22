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
 * JSON-RPC 2.0 Response Object
 * <p>
 * When a rpc call is made, the Server MUST reply with a Response, except for in the case of Notifications.
 */
public class JsonRpcResponse implements Serializable {

    private static final String JSON_RPC_VERSION = "2.0";

    private static final long serialVersionUID = 1L;

    /**
     * A String specifying the version of the JSON-RPC protocol. MUST be exactly "2.0".
     */
    private String jsonrpc = JSON_RPC_VERSION;

    /**
     * This member is REQUIRED on success.
     * This member MUST NOT exist if there was an error invoking the method.
     * The value of this member is determined by the method invoked on the Server.
     */
    private Object result;

    /**
     * This member is REQUIRED on error.
     * This member MUST NOT exist if there was no error triggered during invocation.
     */
    private JsonRpcError error;

    /**
     * This member is REQUIRED.
     * It MUST be the same as the value of the id member in the Request Object.
     * If there was an error in detecting the id in the Request object (e.g. Parse error/Invalid Request),
     * it MUST be Null.
     */
    private Object id;

    public JsonRpcResponse() {
    }

    public JsonRpcResponse(Object result, Object id) {
        this.result = result;
        this.id = id;
    }

    public JsonRpcResponse(JsonRpcError error, Object id) {
        this.error = error;
        this.id = id;
    }

    public String getJsonrpc() {
        return jsonrpc;
    }

    public void setJsonrpc(String jsonrpc) {
        this.jsonrpc = jsonrpc;
    }

    public Object getResult() {
        return result;
    }

    public void setResult(Object result) {
        this.result = result;
    }

    public JsonRpcError getError() {
        return error;
    }

    public void setError(JsonRpcError error) {
        this.error = error;
    }

    public Object getId() {
        return id;
    }

    public void setId(Object id) {
        this.id = id;
    }

    /**
     * Check if this response indicates success
     *
     * @return true if this is a successful response
     */
    public boolean isSuccess() {
        return error == null;
    }

    /**
     * Check if this response indicates an error
     *
     * @return true if this is an error response
     */
    public boolean isError() {
        return error != null;
    }

    /**
     * Create a notification response
     */
    public static JsonRpcResponse createNotificationResponse() {
        return new JsonRpcResponse();
    }

    /**
     * Create a successful response
     */
    public static JsonRpcResponse createSuccessResponse(Object id, Object result) {
        JsonRpcResponse response = new JsonRpcResponse();
        response.setId(id);
        response.setResult(result);
        return response;
    }

    /**
     * Create an error response
     */
    public static JsonRpcResponse createErrorResponse(Object id, JsonRpcError error) {
        JsonRpcResponse response = new JsonRpcResponse();
        response.setId(id);
        response.setError(error);
        return response;
    }

    /**
     * Create an error response with error code and message
     */
    public static JsonRpcResponse createErrorResponse(Object id, int errorCode, String errorMessage) {
        return createErrorResponse(id, new JsonRpcError(errorCode, errorMessage));
    }

    /**
     * Create a parse error response
     */
    public static JsonRpcResponse createParseErrorResponse() {
        return createErrorResponse(null, JsonRpcError.parseError());
    }

    /**
     * Create an invalid request error response
     */
    public static JsonRpcResponse createInvalidRequestResponse(Object id) {
        return createErrorResponse(id, JsonRpcError.invalidRequest());
    }

    /**
     * Create a method not found error response
     */
    public static JsonRpcResponse createMethodNotFoundResponse(Object id) {
        return createErrorResponse(id, JsonRpcError.methodNotFound());
    }

    /**
     * Create an invalid params error response
     */
    public static JsonRpcResponse createInvalidParamsResponse(Object id) {
        return createErrorResponse(id, JsonRpcError.invalidParams());
    }

    /**
     * Create an server error response
     */
    public static JsonRpcResponse createServerErrorResponse(Object id, String errorMsg) {
        return createErrorResponse(id, JsonRpcError.serverError(errorMsg));
    }

    /**
     * Create an server error response
     */
    public static JsonRpcResponse createServerErrorResponse(Object id, int errorCode, String errorMsg) {
        return createErrorResponse(id, JsonRpcError.serverError(errorCode, errorMsg));
    }

    /**
     * Create an server error response
     */
    public static JsonRpcResponse createServerErrorResponse(Object id, Throwable e) {
        if (e instanceof JsonRpcException) {
            createErrorResponse(id, JsonRpcError.serverError(((JsonRpcException) e).getCode(), e.getMessage()));
        }
        return createErrorResponse(id, JsonRpcError.serverError(e.getMessage()));
    }

    @Override
    public String toString() {
        return "JsonRpcResponse{" +
                "jsonrpc='" + jsonrpc + '\'' +
                ", result=" + result +
                ", error=" + error +
                ", id=" + id +
                '}';
    }
}