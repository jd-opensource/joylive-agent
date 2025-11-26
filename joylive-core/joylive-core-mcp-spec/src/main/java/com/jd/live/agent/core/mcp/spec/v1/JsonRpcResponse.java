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
package com.jd.live.agent.core.mcp.spec.v1;

import com.jd.live.agent.core.mcp.exception.JsonRpcException;
import lombok.*;

import java.io.Serializable;

/**
 * A successful (non-error) response to a request.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class JsonRpcResponse implements JsonRpcMessage {

    private static final long serialVersionUID = 1L;

    /**
     * The JSON-RPC version (must be "2.0")
     */
    private String jsonrpc = JSON_RPC_VERSION;

    /**
     * The request identifier that this response corresponds to
     */
    private Object id;

    /**
     * The result of the successful request
     */
    private Object result;

    /**
     * Error information if the request failed
     */
    private JsonRpcError error;

    public JsonRpcResponse(Object id, Object result) {
        this(JSON_RPC_VERSION, id, result, null);
    }

    public JsonRpcResponse(Object id, JsonRpcError error) {
        this(JSON_RPC_VERSION, id, null, error);
    }

    /**
     * Check if this response indicates success
     *
     * @return true if this is a successful response
     */
    public boolean success() {
        return error == null;
    }

    /**
     * Check if this response indicates an error
     *
     * @return true if this is an error response
     */
    public boolean error() {
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
        return new JsonRpcResponse(id, result);
    }

    /**
     * Create an error response
     */
    public static JsonRpcResponse createErrorResponse(Object id, JsonRpcError error) {
        return new JsonRpcResponse(id, error);
    }

    /**
     * Create an error response with error code and message
     */
    public static JsonRpcResponse createErrorResponse(Object id, int errorCode, String errorMessage) {
        return createErrorResponse(id, new JsonRpcError(errorCode, errorMessage));
    }

    /**
     * Create an server error response
     */
    public static JsonRpcResponse createErrorResponse(Object id, Throwable e) {
        if (e instanceof JsonRpcException) {
            createErrorResponse(id, new JsonRpcError(((JsonRpcException) e).getCode(), e.getMessage()));
        }
        return createErrorResponse(id, JsonRpcError.serverError(e.getMessage() == null ? e.getClass().getName() : e.getMessage()));
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
     * Create an invalid request error response
     */
    public static JsonRpcResponse createInvalidRequestResponse(String message) {
        return createErrorResponse(null, ErrorCodes.INVALID_REQUEST, message);
    }

    /**
     * Create an invalid request error response
     */
    public static JsonRpcResponse createInvalidRequestResponse(Object id, String message) {
        return createErrorResponse(id, ErrorCodes.INVALID_REQUEST, message);
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

    @Override
    public String toString() {
        return "JsonRpcResponse{" +
                "jsonrpc='" + jsonrpc + '\'' +
                ", result=" + result +
                ", error=" + error +
                ", id=" + id +
                '}';
    }

    /**
     * A response to a request that indicates an error occurred.
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class JsonRpcError implements Serializable {

        private static final long serialVersionUID = 1L;

        /**
         * The error type that occurred
         */
        private int code;

        /**
         * A short description of the error. The message SHOULD be limited to a concise single sentence
         */
        private String message;

        /**
         * Additional information about the error. The value of this member is defined by the sender
         * (e.g. detailed error information, nested errors etc.)
         */
        private Object data;

        public JsonRpcError(int code, String message) {
            this(code, message, null);
        }

        /**
         * Create a parse error
         */
        public static JsonRpcError parseError() {
            return new JsonRpcError(ErrorCodes.PARSE_ERROR, "Parse error");
        }

        /**
         * Create an invalid request error
         */
        public static JsonRpcError invalidRequest() {
            return new JsonRpcError(ErrorCodes.INVALID_REQUEST, "Invalid Request");
        }

        /**
         * Create a method not found error
         */
        public static JsonRpcError methodNotFound() {
            return new JsonRpcError(ErrorCodes.METHOD_NOT_FOUND, "Method not found");
        }

        /**
         * Create an invalid params error
         */
        public static JsonRpcError invalidParams() {
            return new JsonRpcError(ErrorCodes.INVALID_PARAMS, "Invalid params");
        }

        /**
         * Create an internal error
         */
        public static JsonRpcError internalError() {
            return new JsonRpcError(ErrorCodes.INTERNAL_ERROR, "Internal error");
        }

        /**
         * Create a server error with custom message
         */
        public static JsonRpcError serverError(String message) {
            return new JsonRpcError(ErrorCodes.SERVER_ERROR_MAX, message);
        }

        /**
         * Create a server error with custom code and message
         */
        public static JsonRpcError serverError(int code, String message) {
            if (code < ErrorCodes.SERVER_ERROR_MIN || code > ErrorCodes.SERVER_ERROR_MAX) {
                throw new IllegalArgumentException("Server error code must be between " + ErrorCodes.SERVER_ERROR_MIN + " and " + ErrorCodes.SERVER_ERROR_MAX);
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
}