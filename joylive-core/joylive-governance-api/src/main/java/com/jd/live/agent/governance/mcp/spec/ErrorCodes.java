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

/**
 * Standard error codes used in MCP JSON-RPC responses.
 */
public class ErrorCodes {
    /**
     * Invalid JSON was received by the server.
     */
    public static final int PARSE_ERROR = -32700;
    /**
     * The JSON sent is not a valid Request object.
     */
    public static final int INVALID_REQUEST = -32600;
    /**
     * The method does not exist / is not available.
     */
    public static final int METHOD_NOT_FOUND = -32601;
    /**
     * Invalid method parameter(s).
     */
    public static final int INVALID_PARAMS = -32602;
    /**
     * Internal JSON-RPC error.
     */
    public static final int INTERNAL_ERROR = -32603;
    /**
     * Resource not found.
     */
    public static final int RESOURCE_NOT_FOUND = -32002;

    // Server error codes range: -32000 to -32099
    public static final int SERVER_ERROR_MIN = -32099;
    public static final int SERVER_ERROR_MAX = -32000;
}
