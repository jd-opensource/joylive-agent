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
package com.jd.live.agent.plugin.application.springboot.v2.mcp.web.jakarta;

import com.jd.live.agent.core.parser.jdk.ReflectionJsonSchemaParser;
import com.jd.live.agent.governance.mcp.McpToolScanner;
import com.jd.live.agent.governance.mcp.handler.McpHandler;
import com.jd.live.agent.governance.mcp.spec.JsonRpcRequest;
import com.jd.live.agent.governance.mcp.spec.JsonRpcResponse;
import com.jd.live.agent.governance.mcp.spec.Request;
import com.jd.live.agent.plugin.application.springboot.v2.mcp.AbstractMcpController;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.WebRequest;

import java.util.Map;

import static com.jd.live.agent.core.util.ExceptionUtils.getCause;

/**
 * Jakarta Servlet API implementation of MCP controller.
 * Handles JSON-RPC requests using Jakarta Servlet API.
 */
@RestController
@RequestMapping("${mcp.path:${CONFIG_MCP_PATH:/mcp}}")
public class JakartaWebMcpController extends AbstractMcpController {

    /**
     * Bean name for this controller
     */
    public static final String NAME = "webMcpController";

    /**
     * Gets all Spring REST controllers from the application context
     *
     * @param context The Spring application context
     * @return Map of controller bean names to controller instances
     */
    @Override
    protected Map<String, Object> getControllers(ConfigurableApplicationContext context) {
        return context.getBeansWithAnnotation(RestController.class);
    }

    /**
     * Creates a Jakarta MCP tool scanner for this controller
     *
     * @param context The Spring application context
     * @return A new JakartaWebMcpToolScanner instance
     */
    @Override
    protected McpToolScanner createScanner(ConfigurableApplicationContext context) {
        return new JakartaWebMcpToolScanner(context.getBeanFactory());
    }

    /**
     * Handles incoming JSON-RPC requests
     *
     * @param request      The JSON-RPC request
     * @param webRequest   The Spring web request
     * @param httpRequest  The Jakarta HTTP servlet request
     * @param httpResponse The Jakarta HTTP servlet response
     * @return The JSON-RPC response
     */
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public JsonRpcResponse handle(@RequestBody JsonRpcRequest request,
                                  WebRequest webRequest,
                                  HttpServletRequest httpRequest,
                                  HttpServletResponse httpResponse) {
        try {
            if (!request.validate()) {
                return JsonRpcResponse.createInvalidRequestResponse(request.getId());
            }
            McpHandler handler = handlers.get(request.getMethod());
            if (handler == null) {
                return JsonRpcResponse.createMethodNotFoundResponse(request.getId());
            }
            String version = null;
            Cookie[] cookies = httpRequest.getCookies();
            if (cookies != null) {
                for (Cookie cookie : cookies) {
                    if (cookie.getName().equalsIgnoreCase(Request.KEY_VERSION)) {
                        version = cookie.getValue();
                    }
                }
            }
            JakartaRequestContext ctx = JakartaRequestContext.builder()
                    .methods(methods)
                    .paths(paths)
                    .converter(objectConverter)
                    .jsonSchemaParser(ReflectionJsonSchemaParser.INSTANCE)
                    .version(getVersion(version))
                    .openApi(openApi)
                    .webRequest(webRequest)
                    .httpRequest(httpRequest)
                    .httpResponse(httpResponse)
                    .build();
            return handler.handle(request, ctx);
        } catch (Throwable e) {
            return JsonRpcResponse.createErrorResponse(request.getId(), getCause(e));
        }
    }
}