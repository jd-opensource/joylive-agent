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
package com.jd.live.agent.plugin.application.springboot.v2.mcp.controller;

import com.jd.live.agent.core.parser.ObjectConverter;
import com.jd.live.agent.governance.mcp.McpParameterConverter;
import com.jd.live.agent.governance.mcp.McpToolMethod;
import com.jd.live.agent.governance.mcp.McpToolScanner;
import com.jd.live.agent.plugin.application.springboot.v2.util.SpringUtils;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.jd.live.agent.core.util.type.ClassUtils.getDeclaredMethod;

/**
 * Base controller for MCP (Method Call Protocol) implementation.
 * Scans and registers methods from Spring controllers during application startup.
 */
public abstract class AbstractMcpController implements ApplicationListener<ApplicationStartedEvent> {

    protected McpToolScanner scanner;

    protected ObjectConverter objectConverter;

    protected McpParameterConverter parameterConverter;

    protected final Map<String, McpToolMethod> methods = new HashMap<>();

    public AbstractMcpController(McpToolScanner scanner,
                                 ObjectConverter objectConverter,
                                 McpParameterConverter parameterConverter) {
        this.scanner = scanner;
        this.objectConverter = objectConverter;
        this.parameterConverter = parameterConverter;
    }

    @Override
    public void onApplicationEvent(ApplicationStartedEvent event) {
        Map<String, Object> controllers = getControllers(event.getApplicationContext());
        Class<?> thisClass = this.getClass();
        for (Object controller : controllers.values()) {
            if (thisClass.isInstance(controller)) {
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

    /**
     * Gets all Spring controllers to be scanned.
     *
     * @return Map of controller beans
     */
    protected abstract Map<String, Object> getControllers(ApplicationContext context);

    public void setObjectConverter(ObjectConverter objectConverter) {
        this.objectConverter = objectConverter;
    }

}