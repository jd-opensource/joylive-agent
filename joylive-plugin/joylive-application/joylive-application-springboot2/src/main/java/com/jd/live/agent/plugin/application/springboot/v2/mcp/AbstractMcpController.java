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
import com.jd.live.agent.core.util.cache.LazyObject;
import com.jd.live.agent.governance.config.GovernanceConfig;
import com.jd.live.agent.governance.mcp.McpToolMethod;
import com.jd.live.agent.governance.mcp.McpToolScanner;
import com.jd.live.agent.governance.mcp.McpVersion;
import com.jd.live.agent.governance.mcp.handler.McpHandler;
import com.jd.live.agent.governance.openapi.OpenApi;
import com.jd.live.agent.plugin.application.springboot.v2.context.SpringAppContext;
import com.jd.live.agent.plugin.application.springboot.v2.util.SpringUtils;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.event.EventListener;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.jd.live.agent.core.util.type.ClassUtils.getDeclaredMethod;

/**
 * Base controller for MCP (Method Call Protocol) implementation.
 * Scans and registers methods from Spring controllers during application startup.
 */
public abstract class AbstractMcpController {

    /**
     * MCP handler mapping, keyed by method name
     */
    protected Map<String, McpHandler> handlers;

    /**
     * Object converter for request and response transformation
     */
    protected ObjectConverter objectConverter;

    /**
     * Governance configuration
     */
    protected GovernanceConfig config;

    /**
     * MCP version mapping, keyed by version identifier
     */
    protected Map<String, McpVersion> versions;

    /**
     * Default MCP version
     */
    protected McpVersion defaultVersion;

    /**
     * Lazy-loaded OpenAPI object
     */
    protected LazyObject<OpenApi> openApi;

    /**
     * Method mapping, keyed by method name
     */
    protected final Map<String, McpToolMethod> methods = new HashMap<>();

    /**
     * Path mapping, keyed by URL path
     */
    protected final Map<String, McpToolMethod> paths = new HashMap<>();

    /**
     * Sets MCP handler mapping
     *
     * @param handlers handler mapping
     */
    public void setHandlers(Map<String, McpHandler> handlers) {
        this.handlers = handlers;
    }

    /**
     * Sets object converter
     *
     * @param objectConverter object converter
     */
    public void setObjectConverter(ObjectConverter objectConverter) {
        this.objectConverter = objectConverter;
    }

    /**
     * Sets governance configuration
     *
     * @param config governance configuration
     */
    public void setConfig(GovernanceConfig config) {
        this.config = config;
    }

    /**
     * Sets MCP version mapping
     *
     * @param versions version mapping
     */
    public void setVersions(Map<String, McpVersion> versions) {
        this.versions = versions;
    }

    /**
     * Sets default MCP version
     *
     * @param defaultVersion default version
     */
    public void setDefaultVersion(McpVersion defaultVersion) {
        this.defaultVersion = defaultVersion;
    }

    /**
     * Gets specified version or returns default if not found
     *
     * @param version version identifier
     * @return MCP version
     */
    public McpVersion getVersion(String version) {
        McpVersion result = version == null ? null : versions.get(version);
        return result == null ? defaultVersion : result;
    }

    /**
     * Application start event handler, scans and registers controller methods
     *
     * @param event application started event
     */
    @EventListener
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
     * Application ready event handler, initializes OpenAPI
     *
     * @param event application ready event
     */
    @EventListener
    public void onApplicationEvent(ApplicationReadyEvent event) {
        SpringUtils.addOpenApiHiddenControllers(this.getClass());
        openApi = LazyObject.of(SpringUtils.getOpenApi(new SpringAppContext(event.getApplicationContext())));
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