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
import com.jd.live.agent.core.util.http.HttpStatus;
import com.jd.live.agent.plugin.application.springboot.mcp.web.AbstractMcpController;
import com.jd.live.agent.plugin.application.springboot.mcp.web.SseEmitterMcpTransport;
import com.jd.live.agent.plugin.application.springboot.mcp.web.javax.JavaxWebMcpController;
import com.jd.live.agent.plugin.application.springboot.mcp.web.javax.JavaxWebMcpToolScanner;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static com.jd.live.agent.core.mcp.McpSession.HEADER_SESSION_ID;
import static com.jd.live.agent.core.mcp.McpSession.QUERY_SESSION_ID;
import static com.jd.live.agent.core.mcp.spec.v1.JsonRpcResponse.*;
import static com.jd.live.agent.core.util.StringUtils.choose;
import static com.jd.live.agent.core.util.StringUtils.isEmpty;
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

    /**
     * Establishes an SSE connection for MCP communication.
     *
     * @param sessionId1 Session ID from request parameter
     * @param sessionId2 Session ID from request header
     * @param request    HTTP servlet request
     * @param response   HTTP servlet response
     * @return SseEmitter for the established connection or null if application is not ready
     * @throws Exception If connection establishment fails
     */
    @GetMapping
    public SseEmitter connect(@RequestParam(value = QUERY_SESSION_ID, required = false) String sessionId1,
                              @RequestHeader(value = HEADER_SESSION_ID, required = false) String sessionId2,
                              HttpServletRequest request,
                              HttpServletResponse response) throws Exception {
        // some client does not provide content type MediaType.TEXT_EVENT_STREAM_VALUE
        if (context.getApplication().getStatus() != AppStatus.READY) {
            onAppNotReady(response);
            return null;
        }
        McpSession session = getOrCreateSession(choose(sessionId1, sessionId2));
        McpTransport transport = session.getTransport();
        if (transport == null) {
            transport = session.connect();
            String url = request.getRequestURI() + "?" + QUERY_SESSION_ID + "=" + session.getId();
            transport.send(null, EventType.ENDPOINT, url);
        }
        return transport.getConnection();
    }

    /**
     * Handles incoming JSON-RPC requests
     *
     * @param request      The JSON-RPC request
     * @param sessionId1   The header session id
     * @param sessionId2   The query session id
     * @param webRequest   The Spring web request
     * @param httpRequest  The Javax HTTP servlet request
     * @param httpResponse The Javax HTTP servlet response
     * @return The JSON-RPC response
     */
    @PostMapping
    public void handle(@RequestBody JsonRpcRequest request,
                       @RequestParam(value = QUERY_SESSION_ID, required = false) String sessionId1,
                       @RequestHeader(value = HEADER_SESSION_ID, required = false) String sessionId2,
                       WebRequest webRequest,
                       HttpServletRequest httpRequest,
                       HttpServletResponse httpResponse) throws Exception {
        if (context.getApplication().getStatus() != AppStatus.READY) {
            onAppNotReady(httpResponse);
            return;
        }
        // get transport and session
        String sessionId = choose(sessionId1, sessionId2);
        McpHandler handler = handlers.get(request.getMethod());
        McpSession session = getOrCreateSession(sessionId, handler, httpResponse);
        if (session == null) {
            onMissingSession(sessionId, httpResponse);
            return;
        }
        // validate and invoke
        JsonRpcResponse response;
        if (!request.validate()) {
            response = createInvalidRequestResponse(request.getId());
        } else if (handler == null) {
            response = createMethodNotFoundResponse(request.getId());
        } else {
            try {
                response = handler.handle(request, createContext(session, webRequest, httpRequest, httpResponse));
            } catch (McpException e) {
                response = createErrorResponse(request.getId(), e.getCause());
            }
        }
        // reply
        reply(request, response, session, httpResponse);
    }

    @Override
    protected Map<String, Object> getControllers(ConfigurableApplicationContext context) {
        return context.getBeansWithAnnotation(RestController.class);
    }

    @Override
    protected McpToolScanner createScanner(ConfigurableApplicationContext context) {
        return new JavaxWebMcpToolScanner(context.getBeanFactory());
    }

    @Override
    protected McpTransport createTransport(McpSession session) {
        long timeout = config.getMcpConfig().getTimeout();
        SseEmitter emitter = timeout > 0 ? new SseEmitter(timeout) : new SseEmitter();
        return new SseEmitterMcpTransport(emitter, session.getId());
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
                .interceptor(interceptor)
                .webRequest(webRequest)
                .httpRequest(request)
                .httpResponse(response)
                .build();
    }

    /**
     * Retrieves an existing MCP session or creates a new one.
     *
     * @param sessionId The session identifier
     * @param handler   The MCP request handler
     * @param response  The HTTP servlet response
     * @return The MCP session associated with the request, or null if unavailable
     */
    private McpSession getOrCreateSession(String sessionId, McpHandler handler, HttpServletResponse response) {
        McpSession session = isEmpty(sessionId) ? null : sessions.get(sessionId);
        if (session != null) {
            session.setLastAccessedTime(System.currentTimeMillis());
            return session;
        } else if (handler instanceof Initialization) {
            // none sse
            session = createSession(sessionId);
            sessions.put(session.getId(), session);
            response.setHeader(HEADER_SESSION_ID, session.getId());
        }
        return session;
    }

    /**
     * Handles requests when the application is not ready to process them.
     *
     * @param response The HTTP response object
     * @throws IOException If an I/O error occurs during response writing
     */
    private void onAppNotReady(HttpServletResponse response) throws IOException {
        writeAndFlush(response, TEXT_PLAIN_VALUE, HttpStatus.SERVICE_UNAVAILABLE.value(), "SSE Server is not ready.");
    }

    /**
     * Handles missing session scenarios according to MCP protocol specifications.
     *
     * @param sessionId The requested session identifier
     * @param response  The HTTP response object
     * @throws IOException If an I/O error occurs during response writing
     */
    private void onMissingSession(String sessionId, HttpServletResponse response) throws IOException {
        if (!isEmpty(sessionId)) {
            // Servers that require a session ID SHOULD respond to requests
            // without an Mcp-Session-Id header (other than initialization) with HTTP 400 Bad Request
            writeAndFlush(response, TEXT_PLAIN_VALUE, HttpStatus.BAD_REQUEST.value(), "SSE without an Mcp-Session-Id header.");
        } else {
            // When a client receives HTTP 404 in response to a request containing an Mcp-Session-Id,
            // it MUST start a new session by sending a new InitializeRequest without a session ID attached
            writeAndFlush(response, TEXT_PLAIN_VALUE, HttpStatus.NOT_FOUND.value(), "SSE session is not found: " + sessionId);
        }
    }

    /**
     * Sends a JSON-RPC response to the client.
     *
     * @param req      The original JSON-RPC request
     * @param resp     The JSON-RPC response to send
     * @param session  The MCP session associated with this request
     * @param response The HTTP servlet response object
     * @throws IOException If an I/O error occurs during response writing
     */
    private void reply(JsonRpcRequest req, JsonRpcResponse resp, McpSession session, HttpServletResponse response) throws IOException {
        McpTransport transport = session.getTransport();
        String sid = session.getId();
        if (resp != null && !req.notification()) {
            // only text event stream
            String data = objectParser.write(resp);
            if (transport == null) {
                // none sse
                writeAndFlush(response, MediaType.APPLICATION_JSON_VALUE, HttpStatus.OK.value(), data);
                return;
            }
            if (transport != null) {
                // sse
                transport.send(req.getId().toString(), EventType.MESSAGE, data)
                        .whenComplete((v, e) -> {
                            if (e != null) {
                                logger.error("SSE Failed to send message to session {}: {}", sid, e.getMessage());
                            }
                        });
            }
        } else if (transport != null && transport.isIdle() && req.heartbeat()) {
            // keep transport heartbeat
            transport.send(null, EventType.HEARTBEAT, null);
        }
        response.setStatus(HttpStatus.ACCEPTED.value());
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
        byte[] bytes = message.getBytes(StandardCharsets.UTF_8);
        response.setStatus(status);
        response.setContentType(contentType);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentLength(bytes.length);
        ServletOutputStream os = response.getOutputStream();
        os.write(bytes);
        os.flush();
    }
}