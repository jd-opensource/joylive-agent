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
package com.jd.live.agent.plugin.application.springboot.mcp;

import com.jd.live.agent.core.instance.Application;
import com.jd.live.agent.core.mcp.*;
import com.jd.live.agent.core.mcp.McpSession.DefaultMcpSession;
import com.jd.live.agent.core.mcp.McpSessionManager.DefaultMcpSessionManager;
import com.jd.live.agent.core.mcp.exception.McpException;
import com.jd.live.agent.core.mcp.handler.McpHandler;
import com.jd.live.agent.core.mcp.spec.v1.Implementation;
import com.jd.live.agent.core.mcp.spec.v1.ServerCapabilities;
import com.jd.live.agent.core.mcp.version.McpVersion;
import com.jd.live.agent.core.openapi.spec.v3.OpenApi;
import com.jd.live.agent.core.openapi.spec.v3.PathItem;
import com.jd.live.agent.core.parser.ObjectConverter;
import com.jd.live.agent.core.parser.ObjectParser;
import com.jd.live.agent.governance.config.GovernanceConfig;
import com.jd.live.agent.governance.config.McpConfig;
import com.jd.live.agent.governance.invoke.InboundInvocation.HttpInboundInvocation;
import com.jd.live.agent.governance.invoke.InvocationContext;
import com.jd.live.agent.plugin.application.springboot.context.SpringAppContext;
import com.jd.live.agent.plugin.application.springboot.mcp.request.McpInboundRequest;
import com.jd.live.agent.plugin.application.springboot.util.SpringUtils;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.event.EventListener;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;

import static com.jd.live.agent.core.mcp.McpSession.HEADER_SESSION_ID;
import static com.jd.live.agent.core.mcp.McpSession.QUERY_SESSION_ID;
import static com.jd.live.agent.core.util.StringUtils.choose;
import static com.jd.live.agent.core.util.StringUtils.isEmpty;
import static com.jd.live.agent.core.util.type.ClassUtils.getDeclaredMethod;

/**
 * Base controller for MCP (Method Call Protocol) implementation.
 * Scans and registers methods from Spring controllers during application startup.
 */
public abstract class AbstractMcpController {

    private static final Logger logger = LoggerFactory.getLogger(AbstractMcpController.class);

    /**
     * Governance invocation context
     */
    @Setter
    protected InvocationContext context;

    /**
     * MCP handler mapping, keyed by method name
     */
    @Setter
    protected Map<String, McpHandler> handlers;

    /**
     * Object converter for request and response transformation
     */
    @Setter
    protected ObjectConverter objectConverter;

    @Setter
    protected ObjectParser objectParser;

    /**
     * Governance configuration
     */
    @Setter
    protected GovernanceConfig config;

    /**
     * MCP version mapping, keyed by version identifier
     */
    @Setter
    protected Map<String, McpVersion> versions;

    protected final McpTransportFactory transportFactory = s -> configure(createTransport(s));

    protected final McpToolInterceptor interceptor = this::intercept;

    /**
     * sessions for none sse
     */
    protected final McpSessionManager sessions = new DefaultMcpSessionManager();

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
    protected final Map<String, List<McpToolMethod>> paths = new HashMap<>();

    protected final CompletableFuture<Void> future = new CompletableFuture<>();

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
        // cleaner
        McpConfig cfg = config.getMcpConfig();
        context.getTimer().schedule("clean-mcp-session", cfg.getCheckInterval(), () -> {
            int count = sessions.evict(cfg.getTimeout());
            if (count > 0) {
                logger.info("Success cleaning expired mcp sessions: {}", count);
            }
        });
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

    /**
     * Delete session
     *
     * @param sessionId1 The header session id
     * @param sessionId2 The query session id
     */
    @DeleteMapping
    public void deleteSession(@RequestParam(value = QUERY_SESSION_ID, required = false) String sessionId1,
                              @RequestHeader(value = HEADER_SESSION_ID, required = false) String sessionId2) {
        String sessionId = choose(sessionId1, sessionId2);
        McpSession session = sessions.remove(sessionId);
        if (session != null) {
            session.close();
        }
    }

    /**
     * Retrieves an existing MCP session or creates a new one if none exists.
     *
     * @param sessionId The session identifier. If null or empty, a new UUID will be generated.
     * @return The existing or newly created McpSession associated with the sessionId
     */
    protected McpSession getOrCreateSession(String sessionId) {
        McpSession session = null;
        if (!isEmpty(sessionId)) {
            session = sessions.get(sessionId);
        } else {
            sessionId = UUID.randomUUID().toString();
        }
        if (session == null) {
            session = createSession(sessionId, null);
            sessions.put(sessionId, session);
        }
        return session;
    }

    /**
     * Creates and initializes a new MCP session with specified parameters.
     *
     * @param sessionId        Unique identifier for the session
     * @return true if session creation succeeded, false if it failed
     */
    protected McpSession createSession(String sessionId) {
        return createSession(sessionId, null);
    }

    /**
     * Creates and initializes a new MCP session with specified parameters.
     *
     * @param sessionId        Unique identifier for the session
     * @param version          The mcp protocol version
     * @return true if session creation succeeded, false if it failed
     */
    protected McpSession createSession(String sessionId, String version) {
        Application application = context.getApplication();
        McpConfig mcpConfig = config.getMcpConfig();
        Implementation serverInfo = Implementation
                .builder()
                .name(application.getName())
                .title(mcpConfig.getTitle())
                .version(isEmpty(application.getVersion()) ? "unknown" : application.getVersion())
                .build();
        ServerCapabilities serverCapabilities = ServerCapabilities.builder()
                // disable logging capabilities for risk
                //.logging(new ServerCapabilities.LoggingCapabilities())
                //.completions(new ServerCapabilities.CompletionCapabilities())
                // TODO tools listChanged
                .tools(new ServerCapabilities.ToolCapabilities(Boolean.FALSE))
                //.prompts(new ServerCapabilities.PromptCapabilities())
                //.resources(new ServerCapabilities.ResourceCapabilities())
                //.experimental(null)
                .build();
        sessionId = isEmpty(sessionId) ? UUID.randomUUID().toString() : sessionId;
        Predicate<String> predicate = v -> v != null && versions.containsKey(v);
        if (!predicate.test(version)) {
            version = mcpConfig.getVersion();
            if (!predicate.test(version)) {
                version = McpVersion.getLatestVersion();
            }
        }
        return new DefaultMcpSession(
                sessionId,
                version,
                serverCapabilities,
                serverInfo,
                mcpConfig.getMetadata(),
                predicate,
                transportFactory
        );
    }

    /**
     * Creates a transport mechanism for the given session.
     *
     * @param session The session requiring transport
     * @return The created transport implementation
     */
    protected abstract McpTransport createTransport(McpSession session);

    /**
     * Configures a transport with completion, timeout, and error handlers.
     *
     * @param transport The transport to configure
     * @return The configured transport, or null if input was null
     */
    protected McpTransport configure(McpTransport transport) {
        if (transport != null) {
            transport.onCompletion(() -> onCompletion(transport));
            transport.onTimeout(() -> onTimeout(transport));
            transport.onError(e -> onError(transport, e));
        }
        return transport;
    }

    /**
     * Handles transport completion events.
     * Removes the session from active sessions, closes the transport,
     * and logs the completion event.
     *
     * @param transport The transport connection that completed
     */
    protected void onCompletion(McpTransport transport) {
        sessions.remove(transport.getId());
        transport.close();
        logger.info("SSE connection is closed by completion for session: {}", transport.getId());
    }

    /**
     * Handles transport error events.
     * Removes the session from active sessions, closes the transport with the error,
     * and logs the error event.
     *
     * @param transport The transport connection that encountered an error
     * @param e         The throwable representing the error
     */
    protected void onError(McpTransport transport, Throwable e) {
        sessions.remove(transport.getId());
        transport.close(e);
        logger.error("SSE connection is closed by error for session: {}", transport.getId(), e);
    }

    /**
     * Handles transport timeout events.
     * Removes the session from active sessions, closes the transport,
     * and logs the timeout event.
     *
     * @param transport The transport connection that timed out
     */
    protected void onTimeout(McpTransport transport) {
        sessions.remove(transport.getId());
        transport.close();
        logger.warn("SSE connection is closed by timed out for session: {}", transport.getId());
    }

    /**
     * Registers a tool method with this controller.
     *
     * @param method  The tool method to register
     * @param openApi Optional OpenAPI specification for path mapping
     */
    protected void addToolMethod(McpToolMethod method, OpenApi openApi) {
        if (openApi == null) {
            // TODO unique
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
                paths.computeIfAbsent(p, v -> new ArrayList<>()).add(method);
            });
        }
    }

    /**
     * Intercepts tool invocations and delegates to the actual controller method.
     *
     * @param invocation The tool invocation context
     * @return The result of the controller method execution
     * @throws Exception If method invocation fails
     */
    protected Object intercept(McpToolInvocation invocation) throws Exception {
        if (!config.getMcpConfig().isGovernanceEnabled()) {
            return invocation.call();
        }
        McpInboundRequest request = new McpInboundRequest(invocation.getRequest(), invocation.getMethod());
        HttpInboundInvocation<McpInboundRequest> inv = new HttpInboundInvocation<>(request, context);
        try {
            return context.inward(inv, invocation::call);
        } catch (Exception e) {
            throw e;
        } catch (Throwable e) {
            throw new McpException(e.getMessage(), e);
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