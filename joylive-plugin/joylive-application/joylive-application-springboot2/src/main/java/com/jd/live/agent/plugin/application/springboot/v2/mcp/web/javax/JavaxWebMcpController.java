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
package com.jd.live.agent.plugin.application.springboot.v2.mcp.web.javax;

import com.jd.live.agent.core.util.ExceptionUtils;
import com.jd.live.agent.governance.jsonrpc.JsonRpcRequest;
import com.jd.live.agent.governance.jsonrpc.JsonRpcResponse;
import com.jd.live.agent.governance.mcp.DefaultMcpParameterParser;
import com.jd.live.agent.governance.mcp.McpToolMethod;
import com.jd.live.agent.plugin.application.springboot.v2.mcp.AbstractMcpController;
import org.springframework.context.ApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.WebRequest;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;

@RestController
@RequestMapping("${mcp.path:${CONFIG_MCP_PATH:/mcp}}")
public class JavaxWebMcpController extends AbstractMcpController {

    public static final String NAME = "webMcpController";

    public JavaxWebMcpController() {
        super(JavaxWebMcpToolScanner.INSTANCE, null, DefaultMcpParameterParser.INSTANCE);
    }

    @Override
    protected Map<String, Object> getControllers(ApplicationContext context) {
        return context.getBeansWithAnnotation(RestController.class);
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public JsonRpcResponse handle(@RequestBody JsonRpcRequest request,
                                  WebRequest webRequest,
                                  HttpServletRequest httpRequest,
                                  HttpServletResponse httpResponse) {
        try {
            McpToolMethod method = methods.get(request.getMethod());
            if (method == null) {
                return JsonRpcResponse.createMethodNotFoundResponse(request.getId());
            }
            Object result = invoke(method, request.getParams(), webRequest, httpRequest, httpResponse);
            return request.notification()
                    ? JsonRpcResponse.createNotificationResponse()
                    : JsonRpcResponse.createSuccessResponse(request.getId(), result);
        } catch (Throwable e) {
            return JsonRpcResponse.createErrorResponse(request.getId(), e);
        }
    }

    private Object invoke(McpToolMethod method,
                          Object params,
                          WebRequest webRequest,
                          HttpServletRequest httpRequest,
                          HttpServletResponse httpResponse) throws Throwable {
        try {
            JavaxParserContext ctx = new JavaxParserContext(webRequest, httpRequest, httpResponse);
            Object[] args = parameterParser.parse(method, params, objectConverter, ctx);
            return method.getMethod().invoke(method.getController(), args);
        } catch (Exception e) {
            throw ExceptionUtils.getCause(e);
        }
    }
}