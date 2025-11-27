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
package com.jd.live.agent.plugin.application.springboot.mcp.reactive;

import com.jd.live.agent.core.exception.InvokeException;
import com.jd.live.agent.core.mcp.McpRequestContext;
import com.jd.live.agent.core.mcp.McpToolScanner;
import com.jd.live.agent.core.mcp.handler.McpHandler;
import com.jd.live.agent.core.mcp.spec.v1.JsonRpcRequest;
import com.jd.live.agent.core.mcp.spec.v1.JsonRpcResponse;
import com.jd.live.agent.core.mcp.spec.v1.Request;
import com.jd.live.agent.core.parser.jdk.ReflectionJsonSchemaParser;
import com.jd.live.agent.plugin.application.springboot.mcp.AbstractMcpController;
import com.jd.live.agent.plugin.application.springboot.mcp.converter.MonoConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.http.HttpCookie;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import static com.jd.live.agent.core.util.ExceptionUtils.getCause;

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
     * Creates a reactive MCP tool scanner for this controller
     *
     * @param context The Spring application context
     * @return A new ReactiveMcpToolScanner instance
     */
    @Override
    protected McpToolScanner createScanner(ConfigurableApplicationContext context) {
        return new ReactiveMcpToolScanner(context.getBeanFactory());
    }

    /**
     * Handles incoming JSON-RPC requests in a reactive manner
     *
     * @param request  The JSON-RPC request as a Mono
     * @param exchange The server web exchange containing request/response information
     * @return A Mono containing the JSON-RPC response
     */
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<JsonRpcResponse> handle(@RequestBody Mono<JsonRpcRequest> request, ServerWebExchange exchange) {
        return request.flatMap(req -> {
            try {
                if (!req.validate()) {
                    return Mono.just(JsonRpcResponse.createInvalidRequestResponse(req.getId()));
                }
                McpHandler handler = handlers.get(req.getMethod());
                if (handler == null) {
                    return Mono.just(JsonRpcResponse.createMethodNotFoundResponse(req.getId()));
                }
                McpRequestContext ctx = createRequestContext(exchange);
                return MonoConverter.INSTANCE.convert(handler.handle(req, ctx))
                        .map(r -> {
                            if (req.notification()) {
                                return JsonRpcResponse.createNotificationResponse();
                            } else if (r instanceof JsonRpcResponse) {
                                return (JsonRpcResponse) r;
                            }
                            return JsonRpcResponse.createSuccessResponse(req.getId(), r);
                        })
                        .onErrorResume(e -> Mono.just(JsonRpcResponse.createErrorResponse(req.getId(), e)));
            } catch (InvokeException e) {
                return Mono.just(JsonRpcResponse.createErrorResponse(req.getId(), e.getCause()));
            } catch (Throwable e) {
                logger.error(e.getMessage(), e);
                return Mono.just(JsonRpcResponse.createErrorResponse(req.getId(), getCause(e)));
            }
        });
    }

    /**
     * Creates a reactive request context from server exchange.
     *
     * @param exchange WebFlux server exchange
     * @return Configured request context for reactive processing
     */
    private McpRequestContext createRequestContext(ServerWebExchange exchange) {
        return ReactiveRequestContext.builder()
                .methods(methods)
                .paths(paths)
                .converter(objectConverter)
                .jsonSchemaParser(ReflectionJsonSchemaParser.INSTANCE)
                .version(getVersion(getMcpVersion(exchange)))
                .openApi(openApi.get())
                .interceptor(this::intercept)
                .exchange(exchange)
                .build();
    }

    /**
     * Extracts MCP version from request cookies.
     *
     * @param exchange The server web exchange containing request/response information
     * @return Version string if found in cookies, null otherwise
     */
    private String getMcpVersion(ServerWebExchange exchange) {
        HttpCookie cookie = exchange.getRequest().getCookies().getFirst(Request.KEY_VERSION);
        return cookie != null ? cookie.getValue() : null;
    }
}