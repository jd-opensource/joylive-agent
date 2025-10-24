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
package com.jd.live.agent.plugin.application.springboot.v2.mcp.controller.web;

import com.jd.live.agent.governance.jsonrpc.JsonRpcException;
import com.jd.live.agent.governance.jsonrpc.JsonRpcRequest;
import com.jd.live.agent.governance.jsonrpc.JsonRpcResponse;
import com.jd.live.agent.governance.mcp.DefaultMcpParameterConverter;
import com.jd.live.agent.governance.mcp.McpToolMethod;
import com.jd.live.agent.plugin.application.springboot.v2.mcp.controller.AbstractMcpController;
import org.springframework.context.ApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("${mcp.path:${CONFIG_MCP_PATH:/mcp}}")
public class WebMcpController extends AbstractMcpController {

    public static final String NAME = "webMcpController";

    public WebMcpController() {
        super(WebMcpToolScanner.INSTANCE, null, DefaultMcpParameterConverter.INSTANCE);
    }

    @Override
    protected Map<String, Object> getControllers(ApplicationContext context) {
        return context.getBeansWithAnnotation(RestController.class);
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public JsonRpcResponse handle(@RequestBody JsonRpcRequest request) {
        try {
            McpToolMethod method = methods.get(request.getMethod());
            if (method == null) {
                return JsonRpcResponse.createMethodNotFoundResponse(request.getId());
            }
            Object result = invokeMethod(method, request.getParams());
            return request.notification()
                    ? JsonRpcResponse.createNotificationResponse()
                    : JsonRpcResponse.createSuccessResponse(request.getId(), result);
        } catch (JsonRpcException e) {
            return JsonRpcResponse.createErrorResponse(request.getId(), e.getCode(), e.getMessage());
        } catch (Throwable e) {
            return JsonRpcResponse.createServerErrorResponse(request.getId(), e.getMessage());
        }
    }

    private Object invokeMethod(McpToolMethod method, Object params) throws Exception {
        return method.getMethod().invoke(method.getController(), parameterConverter.convert(method, params, objectConverter));
    }
}