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

import com.jd.live.agent.core.mcp.McpToolMethod;
import com.jd.live.agent.core.mcp.McpToolScanner;
import com.jd.live.agent.core.mcp.handler.McpHandler;
import com.jd.live.agent.core.mcp.version.McpVersion;
import com.jd.live.agent.core.openapi.spec.v3.OpenApi;
import com.jd.live.agent.core.openapi.spec.v3.PathItem;
import com.jd.live.agent.core.parser.ObjectConverter;
import com.jd.live.agent.core.parser.ObjectParser;
import com.jd.live.agent.governance.config.GovernanceConfig;
import com.jd.live.agent.plugin.application.springboot.v2.context.SpringAppContext;
import com.jd.live.agent.plugin.application.springboot.v2.util.SpringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.event.EventListener;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

import static com.jd.live.agent.core.util.type.ClassUtils.getDeclaredMethod;

/**
 * Base controller for MCP (Method Call Protocol) implementation.
 * Scans and registers methods from Spring controllers during application startup.
 */
public abstract class AbstractMcpController {

    private static final Logger logger = LoggerFactory.getLogger(AbstractMcpController.class);

    /**
     * MCP handler mapping, keyed by method name
     */
    protected Map<String, McpHandler> handlers;

    /**
     * Object converter for request and response transformation
     */
    protected ObjectConverter objectConverter;

    protected ObjectParser objectParser;

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
    protected final AtomicReference<OpenApi> openApi = new AtomicReference<>();

    /**
     * Method mapping, keyed by method name
     */
    protected final Map<String, McpToolMethod> methods = new HashMap<>();

    /**
     * Path mapping, keyed by URL path
     */
    protected final Map<String, McpToolMethod> paths = new HashMap<>();

    protected final CompletableFuture<Void> future = new CompletableFuture();

    public void setHandlers(Map<String, McpHandler> handlers) {
        this.handlers = handlers;
    }

    public void setObjectConverter(ObjectConverter objectConverter) {
        this.objectConverter = objectConverter;
    }

    public void setObjectParser(ObjectParser objectParser) {
        this.objectParser = objectParser;
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
        SpringUtils.addOpenApiHiddenControllers(this.getClass());
        Thread thread = new Thread(() -> {
            try {
                OpenApi value = SpringUtils.getOpenApi(new SpringAppContext(event.getApplicationContext()));
                openApi.set(value);
                Map<String, Object> controllers = getControllers(event.getApplicationContext());
                if (controllers != null && !controllers.isEmpty()) {
                    Class<?> thisClass = this.getClass();
                    McpToolScanner scanner = createScanner(event.getApplicationContext());
                    for (Object controller : controllers.values()) {
                        if (thisClass.isInstance(controller)) {
                            // for flow control interceptor
                            McpToolMethod.HANDLE_METHOD = getDeclaredMethod(controller.getClass(), "handle");
                        } else if (!config.getServiceConfig().isSystemHandler(controller.getClass())) {
                            List<McpToolMethod> values = scanner.scan(controller);
                            if (values != null) {
                                values.forEach(m -> addToolMethod(m, value));
                            }
                        }
                    }
                }
                future.complete(null);
            } catch (Throwable e) {
                future.completeExceptionally(e);
            }
        });
        thread.setDaemon(true);
        thread.start();
    }

    /**
     * Application ready event handler, initializes OpenAPI
     *
     * @param event application ready event
     */
    @EventListener
    public void onApplicationEvent(ApplicationReadyEvent event) {
        try {
            future.get(5000, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (ExecutionException e) {
            logger.warn("Failed to prepare mcp methods.", e);
        } catch (TimeoutException e) {
            logger.warn("It's timeout to prepare mcp methods.");
        }
    }

    protected void addToolMethod(McpToolMethod method, OpenApi openApi) {
        if (openApi == null) {
            methods.put(method.getName(), method);
        }
        if (method.getPaths() != null) {
            method.getPaths().forEach(p -> {
                if (openApi != null) {
                    PathItem pathItem = openApi.getPath(p);
                    if (pathItem != null) {
                        pathItem.operations().forEach(o -> {
                            methods.put(o.getOperationId(), method);
                        });
                    }
                }
                paths.put(p, method);
            });
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