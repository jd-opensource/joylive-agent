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
package com.jd.live.agent.plugin.application.springboot.v2.mcp;

import com.jd.live.agent.core.parser.ObjectConverter;
import com.jd.live.agent.governance.jsonrpc.JsonRpcException;
import com.jd.live.agent.governance.jsonrpc.JsonRpcRequest;
import com.jd.live.agent.governance.jsonrpc.JsonRpcResponse;
import com.jd.live.agent.governance.mcp.DefaultMcpParameterConverter;
import com.jd.live.agent.governance.mcp.McpParameterConverter;
import com.jd.live.agent.governance.mcp.McpToolMethod;
import com.jd.live.agent.governance.mcp.McpToolScanner;
import com.jd.live.agent.plugin.application.springboot.v2.util.SpringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.jd.live.agent.core.util.type.ClassUtils.getDeclaredMethod;

@RestController
@RequestMapping("${CONFIG_MCP_PATH:/mcp}")
public class McpController implements ApplicationListener<ApplicationStartedEvent> {

    public static final String NAME = "mcpController";

    @Autowired
    private ApplicationContext applicationContext;

    private McpToolScanner scanner;

    private ObjectConverter objectConverter;

    private McpParameterConverter parameterConverter;

    private final Map<String, McpToolMethod> methods = new HashMap<>();

    @Override
    public void onApplicationEvent(ApplicationStartedEvent event) {
        McpToolScanner scanner = this.scanner == null ? DefaultMcpToolScanner.INSTANCE : this.scanner;
        Map<String, Object> controllers = applicationContext.getBeansWithAnnotation(RestController.class);
        for (Object controller : controllers.values()) {
            if (controller instanceof McpController) {
                // for flow control interceptor
                McpToolMethod.HANDLE_METHOD = getDeclaredMethod(controller.getClass(), "handle");
            } else if (!SpringUtils.isSystemController(controller)) {
                List<McpToolMethod> values = scanner.scan(controller);
                if (values != null) {
                    values.forEach(m -> methods.put(m.getName(), m));
                }
            }
        }
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public JsonRpcResponse handle(@RequestBody JsonRpcRequest request) {
        try {
            McpToolMethod method = methods.get(request.getMethod());
            if (method == null) {
                return JsonRpcResponse.createMethodNotFoundResponse(request.getId());
            }
            return JsonRpcResponse.createSuccessResponse(request.getId(), invokeMethod(method, request.getParams()));
        } catch (JsonRpcException e) {
            return JsonRpcResponse.createErrorResponse(request.getId(), e.getCode(), e.getMessage());
        } catch (Throwable e) {
            return JsonRpcResponse.createServerErrorResponse(request.getId(), e.getMessage());
        }
    }

    private Object invokeMethod(McpToolMethod method, Object params) throws Exception {
        McpParameterConverter converter = this.parameterConverter == null ? DefaultMcpParameterConverter.INSTANCE : this.parameterConverter;
        return method.getMethod().invoke(method.getController(), converter.convert(method, params, objectConverter));
    }

    public void setParameterConverter(McpParameterConverter parameterConverter) {
        this.parameterConverter = parameterConverter;
    }

    public void setObjectConverter(ObjectConverter objectConverter) {
        this.objectConverter = objectConverter;
    }

    public void setScanner(McpToolScanner scanner) {
        this.scanner = scanner;
    }

    public void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }
}