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
package com.jd.live.agent.plugin.application.springboot.mcp.web.reactive;

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
import com.jd.live.agent.plugin.application.springboot.mcp.converter.MonoConverter;
import com.jd.live.agent.plugin.application.springboot.mcp.web.AbstractMcpController;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static com.jd.live.agent.core.mcp.McpSession.HEADER_SESSION_ID;
import static com.jd.live.agent.core.mcp.McpSession.QUERY_SESSION_ID;
import static com.jd.live.agent.core.mcp.spec.v1.JsonRpcResponse.*;
import static com.jd.live.agent.core.util.StringUtils.choose;
import static com.jd.live.agent.core.util.StringUtils.isEmpty;
import static org.springframework.http.MediaType.TEXT_PLAIN_VALUE;

/**
 * Reactive implementation of MCP controller for handling JSON-RPC requests.
 * Uses Spring WebFlux for non-blocking request processing.
 */
@RestController
@RequestMapping("${mcp.path:${CONFIG_MCP_PATH:/mcp}}")
public class ReactiveMcpController extends AbstractMcpController {

    private static final Logger logger = LoggerFactory.getLogger(ReactiveMcpController.class);

    /**
     * Bean name for this controller
     */
    public static final String NAME = "reactiveMcpController";

    /**
     * Establishes an SSE connection for MCP communication.
     *
     * @param sessionId1 Session ID from request parameter
     * @param sessionId2 Session ID from request header
     * @param exchange   The server web exchange containing request/response information
     * @return SseEmitter for the established connection or null if application is not ready
     */
    @GetMapping
    public Flux<ServerSentEvent<Object>> connect(@RequestParam(value = QUERY_SESSION_ID, required = false) String sessionId1,
                                                 @RequestHeader(value = HEADER_SESSION_ID, required = false) String sessionId2,
                                                 ServerWebExchange exchange) {
        // some client does not provide content type MediaType.TEXT_EVENT_STREAM_VALUE
        if (context.getApplication().getStatus() != AppStatus.READY) {
            onAppNotReady(exchange.getResponse());
            return null;
        }
        McpSession session = getOrCreateSession(choose(sessionId1, sessionId2));
        McpTransport transport = session.getTransport();
        if (transport == null) {
            transport = session.connect();
            String url = exchange.getRequest().getPath() + "?" + QUERY_SESSION_ID + "=" + session.getId();
            transport.send(null, EventType.ENDPOINT, url);
        }
        Sinks.Many<ServerSentEvent<Object>> sink = transport.getConnection();
        return sink.asFlux();
    }

    /**
     * Processes JSON-RPC requests in a reactive manner.
     *
     * @param request    JSON-RPC request body
     * @param sessionId1 Session ID from query parameter
     * @param sessionId2 Session ID from header
     * @param exchange   Server web exchange containing request/response context
     * @return Mono completing when response is sent
     * @throws Exception If request processing fails
     */
    @PostMapping
    public Mono<Void> handle(@RequestBody JsonRpcRequest request,
                             @RequestParam(value = QUERY_SESSION_ID, required = false) String sessionId1,
                             @RequestHeader(value = HEADER_SESSION_ID, required = false) String sessionId2,
                             ServerWebExchange exchange) throws Exception {
        if (context.getApplication().getStatus() != AppStatus.READY) {
            return onAppNotReady(exchange.getResponse());
        }
        // get transport and session
        String sessionId = choose(sessionId1, sessionId2);
        McpHandler handler = handlers.get(request.getMethod());
        McpSession session = getOrCreateSession(sessionId, handler, exchange.getResponse());
        if (session == null) {
            return onMissingSession(sessionId, exchange.getResponse());
        }
        // validate and invoke
        JsonRpcResponse response;
        if (!request.validate()) {
            response = createInvalidRequestResponse(request.getId());
        } else if (handler == null) {
            response = createMethodNotFoundResponse(request.getId());
        } else {
            try {
                response = handler.handle(request, createContext(session, exchange));
            } catch (McpException e) {
                response = createErrorResponse(request.getId(), e.getCause());
            }
        }
        // reply
        return reply(request, response, session, exchange.getResponse());
    }

    @Override
    protected McpToolScanner createScanner(ConfigurableApplicationContext context) {
        return new ReactiveMcpToolScanner(context.getBeanFactory());
    }

    @Override
    protected McpTransport createTransport(McpSession session) {
        return new ReactiveMcpTransport(Sinks.many().multicast().onBackpressureBuffer(), session.getId());
    }

    /**
     * Creates a reactive request context from server exchange.
     *
     * @param exchange WebFlux server exchange
     * @return Configured request context for reactive processing
     */
    private McpRequestContext createContext(McpSession session, ServerWebExchange exchange) {
        return ReactiveRequestContext.builder()
                .session(session)
                .methods(methods)
                .paths(paths)
                .converter(objectConverter)
                .jsonSchemaParser(ReflectionJsonSchemaParser.INSTANCE)
                .version(versions.get(session.getVersion()))
                .openApi(openApi.get())
                .interceptor(interceptor)
                .exchange(exchange)
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
    private McpSession getOrCreateSession(String sessionId, McpHandler handler, ServerHttpResponse response) {
        McpSession session = isEmpty(sessionId) ? null : sessions.get(sessionId);
        if (session != null) {
            session.setLastAccessedTime(System.currentTimeMillis());
            return session;
        } else if (handler instanceof Initialization) {
            // none sse
            session = createSession(sessionId);
            sessions.put(session.getId(), session);
            response.getHeaders().set(HEADER_SESSION_ID, session.getId());
        }
        return session;
    }

    /**
     * Handles requests when the application is not ready to process them.
     *
     * @param response The HTTP response object
     * @throws IOException If an I/O error occurs during response writing
     */
    private Mono<Void> onAppNotReady(ServerHttpResponse response) {
        return writeAndFlush(response, TEXT_PLAIN_VALUE, HttpStatus.SERVICE_UNAVAILABLE.value(), "SSE Server is not ready.");
    }

    /**
     * Handles missing session scenarios according to MCP protocol specifications.
     *
     * @param sessionId The requested session identifier
     * @param response  The HTTP response object
     */
    private Mono<Void> onMissingSession(String sessionId, ServerHttpResponse response) throws IOException {
        if (!isEmpty(sessionId)) {
            // Servers that require a session ID SHOULD respond to requests
            // without an Mcp-Session-Id header (other than initialization) with HTTP 400 Bad Request
            return writeAndFlush(response, TEXT_PLAIN_VALUE, HttpStatus.BAD_REQUEST.value(), "SSE without an Mcp-Session-Id header.");
        } else {
            // When a client receives HTTP 404 in response to a request containing an Mcp-Session-Id,
            // it MUST start a new session by sending a new InitializeRequest without a session ID attached
            return writeAndFlush(response, TEXT_PLAIN_VALUE, HttpStatus.NOT_FOUND.value(), "SSE session is not found: " + sessionId);
        }
    }

    /**
     * Sends a JSON-RPC response to the client.
     *
     * @param req      The original JSON-RPC request
     * @param resp     The JSON-RPC response to send
     * @param session  The MCP session associated with this request
     * @param response The HTTP servlet response object
     */
    private Mono<Void> reply(JsonRpcRequest req, JsonRpcResponse resp, McpSession session, ServerHttpResponse response) {
        McpTransport transport = session.getTransport();
        if (resp != null && !req.notification()) {
            String requestId = req.getId().toString();
            String sessionId = session.getId();
            // Already handled CallToolResult exception in convert
            return MonoConverter.INSTANCE.convert(resp)
                    .flatMap(r -> {
                        JsonRpcResponse value = r instanceof JsonRpcResponse ? (JsonRpcResponse) r : createSuccessResponse(requestId, r);
                        return reply(requestId, sessionId, value, response, transport);
                    })
                    .onErrorResume(e -> reply(requestId, sessionId, createErrorResponse(req.getId(), e), response, transport));
        } else if (transport != null && transport.isIdle() && req.heartbeat()) {
            // keep transport heartbeat
            transport.send(null, EventType.HEARTBEAT, null);
        }
        // Notification or null response, just set status
        response.setRawStatusCode(HttpStatus.ACCEPTED.value());
        return Mono.empty();
    }

    /**
     * Handles the response to a JSON-RPC request.
     *
     * @param requestId The unique identifier of the request
     * @param sessionId The session identifier
     * @param value     The JSON-RPC response to send
     * @param response  The server HTTP response object
     * @param transport The MCP transport for SSE communication, or null if not using SSE
     * @return A Mono that completes when the response has been sent
     */
    private Mono<Void> reply(String requestId,
                             String sessionId,
                             JsonRpcResponse value,
                             ServerHttpResponse response,
                             McpTransport transport) {
        // Prepare response data
        String data = objectParser.write(value);

        if (transport == null) {
            // No SSE transport, send data via current connection
            return writeAndFlush(response, MediaType.APPLICATION_JSON_VALUE, HttpStatus.OK.value(), data);
        }
        // Send data via SSE transport asynchronously (don't wait for completion)
        transport.send(requestId, EventType.MESSAGE, data)
                .whenComplete((v, e) -> {
                    if (e != null) {
                        logger.error("SSE Failed to send message to session {}: {}", sessionId, e.getMessage());
                    }
                });
        // Return immediately with ACCEPTED status
        response.setRawStatusCode(HttpStatus.ACCEPTED.value());
        return Mono.empty();
    }

    /**
     * Writes response with specified status code and message.
     *
     * @param response    The HTTP servlet response
     * @param contentType The HTTP content type
     * @param status      The HTTP status code to set
     * @param message     The error message to write
     */
    private Mono<Void> writeAndFlush(ServerHttpResponse response, String contentType, int status, String message) {
        byte[] bytes = message.getBytes(StandardCharsets.UTF_8);
        HttpHeaders headers = response.getHeaders();
        headers.add(HttpHeaders.CONTENT_TYPE, contentType);
        headers.add(HttpHeaders.CONTENT_ENCODING, StandardCharsets.UTF_8.name());
        headers.add(HttpHeaders.CONTENT_LENGTH, String.valueOf(bytes.length));
        response.setRawStatusCode(status);
        DataBuffer buffer = response.bufferFactory().wrap(bytes);
        return response.writeWith(Mono.just(buffer)).then(Mono.fromRunnable(() -> response.setComplete()));
    }

}