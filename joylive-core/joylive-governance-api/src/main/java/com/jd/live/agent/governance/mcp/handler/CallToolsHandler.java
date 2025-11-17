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

import com.jd.live.agent.core.extension.annotation.Extension;
import com.jd.live.agent.governance.mcp.McpRequestContext;
import com.jd.live.agent.governance.mcp.McpToolMethod;
import com.jd.live.agent.governance.mcp.spec.*;

@Extension(JsonRpcMessage.METHOD_TOOLS_CALL)
public class CallToolsHandler implements McpHandler {

    @Override
    public JsonRpcResponse handle(JsonRpcRequest request, McpRequestContext ctx) throws Exception {
        CallToolRequest req = ctx.convert(request.getParams(), CallToolRequest.class);
        McpToolMethod method = ctx.getToolMethodByName(req.getName());
        if (method == null) {
            return JsonRpcResponse.createMethodNotFoundResponse(request.getId());
        }
        Object[] args = ctx.parse(method, req.getArguments(), ctx);
        Object result = method.getMethod().invoke(method.getController(), args);
        result = ctx.getVersion().output(result);
        CallToolResult response = CallToolResult.builder().isError(false).structuredContent(result).build();
        return JsonRpcResponse.createSuccessResponse(request.getId(), response);
    }
}
