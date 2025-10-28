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

import com.jd.live.agent.governance.jsonrpc.JsonRpcRequest;
import com.jd.live.agent.governance.jsonrpc.JsonRpcResponse;
import com.jd.live.agent.governance.mcp.DefaultMcpParameterParser;
import com.jd.live.agent.governance.mcp.McpToolMethod;
import com.jd.live.agent.governance.mcp.McpToolScanner;
import com.jd.live.agent.plugin.application.springboot.v2.mcp.AbstractMcpController;
import com.jd.live.agent.plugin.application.springboot.v2.mcp.SpringExpressionFactory;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.WebRequest;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;

import static com.jd.live.agent.core.util.ExceptionUtils.getCause;

@RestController
@RequestMapping("${mcp.path:${CONFIG_MCP_PATH:/mcp}}")
public class JavaxWebMcpController extends AbstractMcpController {

    public static final String NAME = "webMcpController";

    public JavaxWebMcpController() {
        super(DefaultMcpParameterParser.INSTANCE);
    }

    @Override
    protected Map<String, Object> getControllers(ConfigurableApplicationContext context) {
        return context.getBeansWithAnnotation(RestController.class);
    }

    @Override
    protected McpToolScanner createScanner(ConfigurableApplicationContext context) {
        return new JavaxWebMcpToolScanner(new SpringExpressionFactory(context.getBeanFactory()));
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public JsonRpcResponse handle(@RequestBody JsonRpcRequest request,
                                  WebRequest webRequest,
                                  HttpServletRequest httpRequest,
                                  HttpServletResponse httpResponse) {
        try {
            if (!request.validate()) {
                return JsonRpcResponse.createInvalidRequestResponse(request.getId());
            }
            McpToolMethod method = methods.get(request.getMethod());
            if (method == null) {
                return JsonRpcResponse.createMethodNotFoundResponse(request.getId());
            }
            JavaxRequestContext ctx = new JavaxRequestContext(objectConverter, webRequest, httpRequest, httpResponse);
            Object[] args = parameterParser.parse(method, request.getParams(), ctx);
            Object result = method.getMethod().invoke(method.getController(), args);
            return request.notification()
                    ? JsonRpcResponse.createNotificationResponse()
                    : JsonRpcResponse.createSuccessResponse(request.getId(), result);
        } catch (Throwable e) {
            return JsonRpcResponse.createErrorResponse(request.getId(), getCause(e));
        }
    }

}