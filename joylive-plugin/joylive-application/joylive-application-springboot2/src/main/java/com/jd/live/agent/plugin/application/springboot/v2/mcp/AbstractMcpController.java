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
import com.jd.live.agent.governance.config.GovernanceConfig;
import com.jd.live.agent.governance.mcp.McpParameterParser;
import com.jd.live.agent.governance.mcp.McpToolMethod;
import com.jd.live.agent.governance.mcp.McpToolScanner;
import com.jd.live.agent.governance.mcp.McpVersion;
import com.jd.live.agent.governance.mcp.handler.McpHandler;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.jd.live.agent.core.util.type.ClassUtils.getDeclaredMethod;

/**
 * Base controller for MCP (Method Call Protocol) implementation.
 * Scans and registers methods from Spring controllers during application startup.
 */
public abstract class AbstractMcpController implements ApplicationListener<ApplicationStartedEvent> {

    protected Map<String, McpHandler> handlers;

    protected ObjectConverter objectConverter;

    protected GovernanceConfig config;

    protected Map<String, McpVersion> versions;

    protected McpVersion defaultVersion;

    protected final McpParameterParser parameterParser;

    protected final Map<String, McpToolMethod> methods = new HashMap<>();

    protected final Map<String, McpToolMethod> paths = new HashMap<>();

    public AbstractMcpController(McpParameterParser parameterParser) {
        this.parameterParser = parameterParser;
    }

    public void setHandlers(Map<String, McpHandler> handlers) {
        this.handlers = handlers;
    }

    public void setObjectConverter(ObjectConverter objectConverter) {
        this.objectConverter = objectConverter;
    }

    public void setConfig(GovernanceConfig config) {
        this.config = config;
    }

    public void setVersions(Map<String, McpVersion> versions) {
        this.versions = versions;
    }

    public void setDefaultVersion(McpVersion defaultVersion) {
        this.defaultVersion = defaultVersion;
    }

    public McpVersion getVersion(String version) {
        McpVersion result = version == null ? null : versions.get(version);
        return result == null ? defaultVersion : result;
    }

    @Override
    public void onApplicationEvent(ApplicationStartedEvent event) {
        Map<String, Object> controllers = getControllers(event.getApplicationContext());
        Class<?> thisClass = this.getClass();
        McpToolScanner scanner = createScanner(event.getApplicationContext());
        if (controllers != null) {
            for (Object controller : controllers.values()) {
                if (thisClass.isInstance(controller)) {
                    // for flow control interceptor
                    McpToolMethod.HANDLE_METHOD = getDeclaredMethod(controller.getClass(), "handle");
                } else if (!config.getServiceConfig().isSystemHandler(controller.getClass())) {
                    List<McpToolMethod> values = scanner.scan(controller);
                    if (values != null) {
                        values.forEach(m -> {
                            methods.put(m.getName(), m);
                            if (m.getPaths() != null) {
                                m.getPaths().forEach(p -> paths.put(p, m));
                            }
                        });
                    }
                }
            }
        }
    }

    /**
     * Gets all Spring controllers to be scanned.
     *
     * @return Map of controller beans
     */
    protected Map<String, Object> getControllers(ConfigurableApplicationContext context) {
        return context.getBeansWithAnnotation(RestController.class);
    }

    /**
     * Creates a scanner to detect MCP tools in the application context.
     *
     * @param context the Spring application context
     * @return the MCP tool scanner instance
     */
    protected abstract McpToolScanner createScanner(ConfigurableApplicationContext context);

}