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
 * JSON-RPC 2.0 Error Object
 * <p>
 * When a rpc call encounters an error, the Response Object MUST contain the error member with a value that is an Object.
 */
public class JsonRpcError implements Serializable {

    private static final long serialVersionUID = 1L;

    // Pre-defined error codes as per JSON-RPC 2.0 specification
    public static final int PARSE_ERROR = -32700;
    public static final int INVALID_REQUEST = -32600;
    public static final int METHOD_NOT_FOUND = -32601;
    public static final int INVALID_PARAMS = -32602;
    public static final int INTERNAL_ERROR = -32603;

    // Server error codes range: -32000 to -32099
    public static final int SERVER_ERROR_MIN = -32099;
    public static final int SERVER_ERROR_MAX = -32000;

    /**
     * A Number that indicates the error type that occurred.
     * This MUST be an integer.
     */
    private int code;

    /**
     * A String providing a short description of the error.
     * The message SHOULD be limited to a concise single sentence.
     */
    private String message;

    /**
     * A Primitive or Structured value that contains additional information about the error.
     * This may be omitted.
     * The value of this member is defined by the Server (e.g. detailed error information, nested errors etc.).
     */
    private Object data;

    public JsonRpcError() {
    }

    public JsonRpcError(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public JsonRpcError(int code, String message, Object data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }

    /**
     * Create a parse error
     */
    public static JsonRpcError parseError() {
        return new JsonRpcError(PARSE_ERROR, "Parse error");
    }

    /**
     * Create an invalid request error
     */
    public static JsonRpcError invalidRequest() {
        return new JsonRpcError(INVALID_REQUEST, "Invalid Request");
    }

    /**
     * Create a method not found error
     */
    public static JsonRpcError methodNotFound() {
        return new JsonRpcError(METHOD_NOT_FOUND, "Method not found");
    }

    /**
     * Create an invalid params error
     */
    public static JsonRpcError invalidParams() {
        return new JsonRpcError(INVALID_PARAMS, "Invalid params");
    }

    /**
     * Create an internal error
     */
    public static JsonRpcError internalError() {
        return new JsonRpcError(INTERNAL_ERROR, "Internal error");
    }

    /**
     * Create a server error with custom message
     */
    public static JsonRpcError serverError(String message) {
        return new JsonRpcError(SERVER_ERROR_MAX, message);
    }

    /**
     * Create a server error with custom code and message
     */
    public static JsonRpcError serverError(int code, String message) {
        if (code < SERVER_ERROR_MIN || code > SERVER_ERROR_MAX) {
            throw new IllegalArgumentException("Server error code must be between " + SERVER_ERROR_MIN + " and " + SERVER_ERROR_MAX);
        }
        return new JsonRpcError(code, message);
    }

    @Override
    public String toString() {
        return "JsonRpcError{" +
                "code=" + code +
                ", message='" + message + '\'' +
                ", data=" + data +
                '}';
    }
}