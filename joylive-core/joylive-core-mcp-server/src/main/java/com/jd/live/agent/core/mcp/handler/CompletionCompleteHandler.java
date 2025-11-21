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
package com.jd.live.agent.core.mcp.handler;

import com.jd.live.agent.core.extension.annotation.Extension;
import com.jd.live.agent.core.mcp.spec.v1.*;
import com.jd.live.agent.core.mcp.McpRequestContext;
import com.jd.live.agent.core.mcp.spec.v1.CompleteResult.CompleteCompletion;

import java.util.ArrayList;

@Extension(JsonRpcMessage.METHOD_COMPLETION_COMPLETE)
public class CompletionCompleteHandler implements McpHandler {

    @Override
    public JsonRpcResponse handle(JsonRpcRequest request, McpRequestContext ctx) throws Exception {
        CompleteRequest req = ctx.convert(request.getParams(), CompleteRequest.class);
        CompleteCompletion completion = new CompleteCompletion(new ArrayList<>(), 0, false);
        return JsonRpcResponse.createSuccessResponse(request.getId(), new CompleteResult(completion));
    }
}
