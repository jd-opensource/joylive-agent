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
package com.jd.live.agent.plugin.application.springboot.v2.mcp.reactive;

import com.jd.live.agent.governance.jsonrpc.JsonRpcRequest;
import com.jd.live.agent.governance.jsonrpc.JsonRpcResponse;
import com.jd.live.agent.governance.mcp.DefaultMcpParameterParser;
import com.jd.live.agent.governance.mcp.McpToolMethod;
import com.jd.live.agent.governance.mcp.McpToolScanner;
import com.jd.live.agent.plugin.application.springboot.v2.mcp.AbstractMcpController;
import com.jd.live.agent.plugin.application.springboot.v2.mcp.SpringExpressionFactory;
import com.jd.live.agent.plugin.application.springboot.v2.mcp.converter.MonoConverter;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import static com.jd.live.agent.core.util.ExceptionUtils.getCause;

@RestController
@RequestMapping("${mcp.path:${CONFIG_MCP_PATH:/mcp}}")
public class ReactiveMcpController extends AbstractMcpController {

    public static final String NAME = "reactiveMcpController";

    public ReactiveMcpController() {
        super(DefaultMcpParameterParser.INSTANCE);
    }

    @Override
    protected McpToolScanner createScanner(ConfigurableApplicationContext context) {
        return new ReactiveMcpToolScanner(new SpringExpressionFactory(context.getBeanFactory()));
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<JsonRpcResponse> handle(@RequestBody Mono<JsonRpcRequest> request, ServerWebExchange exchange) {
        return request.flatMap(req -> {
            if (!req.validate()) {
                return Mono.just(JsonRpcResponse.createInvalidRequestResponse(req.getId()));
            }
            McpToolMethod method = methods.get(req.getMethod());
            if (method == null) {
                return Mono.just(JsonRpcResponse.createMethodNotFoundResponse(req.getId()));
            }
            try {
                Object[] args = parameterParser.parse(method, req.getParams(), objectConverter, new ReactiveParserContext(exchange));
                Object result = method.invoke(args);
                // Result may be a Mono object
                return MonoConverter.INSTANCE.convert(result)
                        .map(r -> req.notification() ? JsonRpcResponse.createNotificationResponse() : JsonRpcResponse.createSuccessResponse(req.getId(), r))
                        .onErrorResume(e -> Mono.just(JsonRpcResponse.createErrorResponse(req.getId(), e)));
            } catch (Throwable e) {
                return Mono.just(JsonRpcResponse.createErrorResponse(req.getId(), getCause(e)));
            }
        });
    }
}