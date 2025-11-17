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
package com.jd.live.agent.governance.mcp.handler;

import com.jd.live.agent.core.extension.annotation.Extensible;
import com.jd.live.agent.governance.mcp.McpRequestContext;
import com.jd.live.agent.governance.mcp.spec.JsonRpcRequest;
import com.jd.live.agent.governance.mcp.spec.JsonRpcResponse;

/**
 * Handler interface for processing MCP (Mesh Configuration Protocol) requests using JSON-RPC format.
 * Implementations of this interface are responsible for processing JSON-RPC requests
 * and generating appropriate responses within the MCP server context.
 */
@Extensible("McpHandler")
public interface McpHandler {

    /**
     * Processes a JSON-RPC request and returns a corresponding response.
     *
     * @param request The JSON-RPC request to be processed
     * @param ctx     The context containing conversion and parsing utilities for the request
     * @return A JSON-RPC response containing the result or error information
     * @throws Exception If an error occurs during request processing
     */
    JsonRpcResponse handle(JsonRpcRequest request, McpRequestContext ctx) throws Exception;

}
