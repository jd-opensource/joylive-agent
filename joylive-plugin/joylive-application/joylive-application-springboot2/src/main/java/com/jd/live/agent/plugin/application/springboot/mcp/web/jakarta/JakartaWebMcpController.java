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
package com.jd.live.agent.plugin.application.springboot.mcp.web.jakarta;

import com.jd.live.agent.bootstrap.logger.Logger;
import com.jd.live.agent.bootstrap.logger.LoggerFactory;
import com.jd.live.agent.core.instance.AppStatus;
import com.jd.live.agent.core.mcp.McpRequestContext;
import com.jd.live.agent.core.mcp.McpSession;
import com.jd.live.agent.core.mcp.McpToolScanner;
import com.jd.live.agent.core.mcp.McpTransport;
import com.jd.live.agent.core.mcp.McpTransport.EventType;
import com.jd.live.agent.core.mcp.exception.McpException;
import com.jd.live.agent.core.mcp.handler.Initialization;
import com.jd.live.agent.core.mcp.handler.McpHandler;
import com.jd.live.agent.core.mcp.spec.v1.JsonRpcRequest;
import com.jd.live.agent.core.mcp.spec.v1.JsonRpcResponse;
import com.jd.live.agent.core.parser.jdk.ReflectionJsonSchemaParser;
import com.jd.live.agent.plugin.application.springboot.mcp.AbstractMcpController;
import com.jd.live.agent.plugin.application.springboot.mcp.web.SseEmitterMcpTransport;
import com.jd.live.agent.plugin.application.springboot.mcp.web.javax.JavaxWebMcpController;
import com.jd.live.agent.plugin.application.springboot.mcp.web.javax.JavaxWebMcpToolScanner;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

import static com.jd.live.agent.core.mcp.McpSession.HEADER_SESSION_ID;
import static com.jd.live.agent.core.mcp.McpTransport.CLIENT_ID;
import static com.jd.live.agent.core.util.StringUtils.isEmpty;
import static jakarta.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static jakarta.servlet.http.HttpServletResponse.SC_SERVICE_UNAVAILABLE;
import static org.springframework.http.MediaType.TEXT_PLAIN_VALUE;

/**
 * Jakarta Servlet API implementation of MCP controller.
 * Handles JSON-RPC requests using Jakarta Servlet API.
 */
@RestController
@RequestMapping("${mcp.path:${CONFIG_MCP_PATH:/mcp}}")
public class JakartaWebMcpController extends AbstractMcpController {

    private static Logger logger = LoggerFactory.getLogger(JavaxWebMcpController.class);

    /**
     * Bean name for this controller
     */
    public static final String NAME = "jakartaWebMcpController";

    @GetMapping
    public SseEmitter connect(@RequestParam(value = CLIENT_ID, required = false) String clientId,
                              HttpServletRequest request,
                              HttpServletResponse response) throws Exception {
        if (context.getApplication().getStatus() != AppStatus.READY) {
            writeAndFlush(response, TEXT_PLAIN_VALUE, SC_SERVICE_UNAVAILABLE, "SSE Server is not ready.");
            return null;
        }
        boolean empty = isEmpty(clientId);
        clientId = empty ? UUID.randomUUID().toString() : clientId;
        long timeout = config.getMcpConfig().getTimeout();
        SseEmitter emitter = timeout > 0 ? new SseEmitter(timeout) : new SseEmitter();
        SseEmitterMcpTransport transport = new SseEmitterMcpTransport(emitter, clientId, this::createSession);
        transports.put(clientId, transport);
        transport.send(null, EventType.ENDPOINT, request.getRequestURI() + "?clientId=" + clientId);
        return emitter;
    }

    /**
     * Delete session
     *
     * @param sessionId The session id
     * @return The JSON-RPC response
     */
    @DeleteMapping
    public void deleteSession(@RequestParam(value = CLIENT_ID, required = false) String clientId,
                              @RequestHeader(value = HEADER_SESSION_ID, required = false) String sessionId) {
        McpTransport transport = transports.get(clientId);
        if (transport == null) {
            McpSession session = transport.removeSession(sessionId);
            if (session != null) {
                session.close();
            }
        }
    }

    /**
     * Handles incoming JSON-RPC requests
     *
     * @param request      The JSON-RPC request
     * @param sessionId    The session id
     * @param webRequest   The Spring web request
     * @param httpRequest  The Javax HTTP servlet request
     * @param httpResponse The Javax HTTP servlet response
     * @return The JSON-RPC response
     */
    @PostMapping
    public void handle(@RequestBody JsonRpcRequest request,
                       @RequestParam(value = CLIENT_ID, required = false) String clientId,
                       @RequestHeader(value = HEADER_SESSION_ID, required = false) String sessionId,
                       WebRequest webRequest,
                       HttpServletRequest httpRequest,
                       HttpServletResponse httpResponse) throws Exception {
        McpTransport transport = transports.get(clientId);
        if (clientId == null || clientId.isEmpty()) {
            writeAndFlush(httpResponse, TEXT_PLAIN_VALUE, SC_SERVICE_UNAVAILABLE, "SSE missing client id.");
            return;
        } else if (transport == null) {
            writeAndFlush(httpResponse, TEXT_PLAIN_VALUE, SC_BAD_REQUEST, "SSE transport is not found: " + clientId);
            return;
        }
        sessionId = isEmpty(sessionId) ? webRequest.getHeader(HEADER_SESSION_ID) : sessionId;
        McpHandler handler = handlers.get(request.getMethod());
        McpSession session = null;
        if (handler instanceof Initialization) {
            session = transport.createSession(sessionId);
            httpResponse.setHeader(HEADER_SESSION_ID, session.getId());
        } else {
            session = transport.getSession(sessionId);
        }

        JsonRpcResponse response;
        try {
            if (session == null) {
                response = JsonRpcResponse.createInvalidRequestResponse(request.getId(), "SSE session is not found: " + sessionId);
            } else if (context.getApplication().getStatus() != AppStatus.READY) {
                response = JsonRpcResponse.createInvalidRequestResponse(request.getId(), "SSE server is not ready.");
            } else if (!request.validate()) {
                response = JsonRpcResponse.createInvalidRequestResponse(request.getId());
            } else if (handler == null) {
                response = JsonRpcResponse.createMethodNotFoundResponse(request.getId());
            } else {
                response = handler.handle(request, createContext(session, webRequest, httpRequest, httpResponse));
            }
        } catch (McpException e) {
            response = JsonRpcResponse.createErrorResponse(request.getId(), e.getCause());
        }
        if (response != null && !request.notification()) {
            // only text event stream
            String id = request.getId().toString();
            String data = objectParser.write(response);
            String sid = session.getId();
            transport.send(id, EventType.MESSAGE, data).whenComplete((v, e) -> {
                if (e != null) {
                    logger.error("SSE Failed to send message to session {}: {}", sid, e.getMessage());
                }
            });
        } else {
            httpResponse.setStatus(HttpServletResponse.SC_ACCEPTED);
        }
    }

    @Override
    protected Map<String, Object> getControllers(ConfigurableApplicationContext context) {
        return context.getBeansWithAnnotation(RestController.class);
    }

    @Override
    protected McpToolScanner createScanner(ConfigurableApplicationContext context) {
        return new JavaxWebMcpToolScanner(context.getBeanFactory());
    }

    /**
     * Creates a request context for handling MCP requests.
     *
     * @param session        Mcp session
     * @param webRequest     Spring web request
     * @param request        HTTP servlet request
     * @param response       HTTP servlet response
     * @return Configured JavaxRequestContext instance
     */
    private McpRequestContext createContext(McpSession session,
                                            WebRequest webRequest,
                                            HttpServletRequest request,
                                            HttpServletResponse response) {
        return JakartaRequestContext.builder()
                .session(session)
                .methods(methods)
                .paths(paths)
                .converter(objectConverter)
                .jsonSchemaParser(ReflectionJsonSchemaParser.INSTANCE)
                .version(versions.get(session.getVersion()))
                .openApi(openApi.get())
                .webRequest(webRequest)
                .httpRequest(request)
                .httpResponse(response)
                .build();
    }

    /**
     * Writes response with specified status code and message.
     *
     * @param response    The HTTP servlet response
     * @param contentType The HTTP content type
     * @param status      The HTTP status code to set
     * @param message     The error message to write
     * @throws IOException If an I/O error occurs while writing the response
     */
    private void writeAndFlush(HttpServletResponse response, String contentType, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType(contentType);
        response.getWriter().write(message);
        response.getWriter().flush();
    }
}