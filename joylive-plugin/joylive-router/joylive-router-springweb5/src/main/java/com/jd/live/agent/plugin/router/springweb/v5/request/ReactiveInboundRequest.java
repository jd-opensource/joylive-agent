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
package com.jd.live.agent.plugin.router.springweb.v5.request;

import com.jd.live.agent.core.parser.JsonPathParser;
import com.jd.live.agent.core.util.http.HttpMethod;
import com.jd.live.agent.core.util.http.HttpUtils;
import com.jd.live.agent.governance.jsonrpc.JsonRpcRequest;
import com.jd.live.agent.governance.jsonrpc.JsonRpcResponse;
import com.jd.live.agent.governance.mcp.McpToolMethod;
import com.jd.live.agent.governance.request.AbstractHttpRequest.AbstractHttpInboundRequest;
import com.jd.live.agent.plugin.router.springweb.v5.util.CloudUtils;
import org.springframework.core.MethodParameter;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpCookie;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.reactive.HandlerResult;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.io.ByteArrayInputStream;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionStage;
import java.util.function.Predicate;

import static com.jd.live.agent.core.util.type.ClassUtils.loadClass;
import static com.jd.live.agent.plugin.router.springweb.v5.exception.SpringInboundThrower.THROWER;

/**
 * ReactiveInboundRequest
 *
 * @author Zhiguo.Chen
 * @since 1.0.0
 */
public class ReactiveInboundRequest extends AbstractHttpInboundRequest<ServerHttpRequest> {

    private static final String ACTUATOR_TYPE = "org.springframework.boot.actuate.endpoint.web.reactive.AbstractWebFluxEndpointHandlerMapping$WebFluxEndpointHandlerMethod";

    private static final Class<?> ACTUATOR_CLASS = loadClass(ACTUATOR_TYPE, ServerWebExchange.class.getClassLoader());

    private static final String RESOURCE_HANDLER_TYPE = "org.springframework.web.reactive.function.server.ResourceHandlerFunction";

    private static final Class<?> RESOURCE_HANDLER_CLASS = loadClass(RESOURCE_HANDLER_TYPE, ServerWebExchange.class.getClassLoader());

    private static final String RESOURCE_WEB_HANDLER_TYPE = "org.springframework.web.reactive.resource.ResourceWebHandler";

    private static final Class<?> RESOURCE_WEB_HANDLER_CLASS = loadClass(RESOURCE_WEB_HANDLER_TYPE, ServerWebExchange.class.getClassLoader());

    private static final String OPEN_API_RESOURCE_TYPE = "org.springdoc.webflux.api.OpenApiResource";

    private static final Class<?> OPEN_API_RESOURCE_CLASS = loadClass(OPEN_API_RESOURCE_TYPE, ServerWebExchange.class.getClassLoader());

    private static final String MULTIPLE_OPEN_API_RESOURCE_TYPE = "org.springdoc.webflux.api.MultipleOpenApiResource";

    private static final Class<?> MULTIPLE_OPEN_API_RESOURCE_CLASS = loadClass(MULTIPLE_OPEN_API_RESOURCE_TYPE, ServerWebExchange.class.getClassLoader());

    private static final String SWAGGER_CONFIG_RESOURCE_TYPE = "org.springdoc.webflux.ui.SwaggerConfigResource";

    private static final Class<?> SWAGGER_CONFIG_RESOURCE_CLASS = loadClass(SWAGGER_CONFIG_RESOURCE_TYPE, ServerWebExchange.class.getClassLoader());

    private static final String SWAGGER_UI_HOME_TYPE = "org.springdoc.webflux.ui.SwaggerUiHome";

    private static final Class<?> SWAGGER_UI_HOME_CLASS = loadClass(SWAGGER_UI_HOME_TYPE, ServerWebExchange.class.getClassLoader());

    private static final String SWAGGER_WELCOME_COMMON_TYPE = "org.springdoc.webflux.ui.SwaggerWelcomeCommon";

    private static final Class<?> SWAGGER_WELCOME_COMMON_CLASS = loadClass(SWAGGER_WELCOME_COMMON_TYPE, ServerWebExchange.class.getClassLoader());

    private static final String SWAGGER2_CONTROLLER_WEBFLUX_TYPE = "springfox.documentation.swagger2.web.Swagger2ControllerWebFlux";

    private static final Class<?> SWAGGER2_CONTROLLER_WEBFLUX_CLASS = loadClass(SWAGGER2_CONTROLLER_WEBFLUX_TYPE, ServerWebExchange.class.getClassLoader());

    private final Predicate<String> systemPredicate;

    private final Predicate<String> mcpPredicate;

    private final Object handler;

    private final JsonPathParser parser;

    public ReactiveInboundRequest(ServerHttpRequest request,
                                  Object handler,
                                  Predicate<String> systemPredicate,
                                  Predicate<String> mcpPredicate,
                                  JsonPathParser parser) {
        super(request);
        this.handler = handler;
        this.systemPredicate = systemPredicate;
        this.mcpPredicate = mcpPredicate;
        this.parser = parser;
        this.uri = request.getURI();
    }

    @Override
    public String getClientIp() {
        String result = super.getClientIp();
        if (result != null && !result.isEmpty()) {
            return result;
        }
        InetSocketAddress address = request.getRemoteAddress();
        return address == null ? null : address.getAddress().getHostAddress();
    }

    @Override
    public boolean isSystem() {
        if (RESOURCE_HANDLER_CLASS != null && RESOURCE_HANDLER_CLASS.isInstance(handler)) {
            return true;
        } else if (RESOURCE_WEB_HANDLER_CLASS != null && RESOURCE_WEB_HANDLER_CLASS.isInstance(handler)) {
            return true;
        } else if (ACTUATOR_CLASS != null && ACTUATOR_CLASS.isInstance(handler)) {
            return true;
        } else if (handler instanceof HandlerMethod) {
            HandlerMethod method = (HandlerMethod) handler;
            Object bean = method.getBean();
            if (OPEN_API_RESOURCE_CLASS != null && OPEN_API_RESOURCE_CLASS.isInstance(bean)
                    || MULTIPLE_OPEN_API_RESOURCE_CLASS != null && MULTIPLE_OPEN_API_RESOURCE_CLASS.isInstance(bean)
                    || SWAGGER_CONFIG_RESOURCE_CLASS != null && SWAGGER_CONFIG_RESOURCE_CLASS.isInstance(bean)
                    || SWAGGER_UI_HOME_CLASS != null && SWAGGER_UI_HOME_CLASS.isInstance(bean)
                    || SWAGGER_WELCOME_COMMON_CLASS != null && SWAGGER_WELCOME_COMMON_CLASS.isInstance(bean)
                    || SWAGGER2_CONTROLLER_WEBFLUX_CLASS != null && SWAGGER2_CONTROLLER_WEBFLUX_CLASS.isInstance(bean)
            ) {
                return true;
            }
        } else if (systemPredicate != null && systemPredicate.test(getPath())) {
            return true;
        }
        return super.isSystem();
    }

    public boolean isMcp() {
        return McpToolMethod.HANDLE_METHOD != null && mcpPredicate != null && mcpPredicate.test(getPath());
    }

    @Override
    public HttpMethod getHttpMethod() {
        org.springframework.http.HttpMethod method = request.getMethod();
        return method == null ? null : HttpMethod.ofNullable(method.name());
    }

    @Override
    public String getCookie(String key) {
        HttpCookie cookie = key == null || key.isEmpty() ? null : request.getCookies().getFirst(key);
        return cookie == null ? null : cookie.getValue();
    }

    @Override
    public String getHeader(String key) {
        return key == null ? null : request.getHeaders().getFirst(key);
    }

    @Override
    public String getQuery(String key) {
        return key == null ? null : request.getQueryParams().getFirst(key);
    }

    @Override
    public List<String> getQueries(String key) {
        return key == null ? null : request.getQueryParams().get(key);
    }

    @Override
    protected Map<String, List<String>> parseHeaders() {
        return CloudUtils.writable(request.getHeaders());
    }

    @Override
    protected Map<String, List<String>> parseQueries() {
        return request.getQueryParams();
    }

    @Override
    protected Map<String, List<String>> parseCookies() {
        return HttpUtils.parseCookie(request.getCookies(), HttpCookie::getValue);
    }

    /**
     * Converts a CompletionStage into a Mono that represents the completion of the stage.
     *
     * @param stage the CompletionStage to convert into a Mono.
     * @return a Mono that represents the completion of the stage.
     */
    public Mono<HandlerResult> convert(CompletionStage<Object> stage) {
        if (isMcp()) {
            return Mono.fromCompletionStage(stage).cast(HandlerResult.class).onErrorResume(this::onMcpErrorResume);
        }
        return Mono.fromCompletionStage(stage).cast(HandlerResult.class).onErrorMap(e -> THROWER.createException(e, this));
    }

    /**
     * Handles MCP errors by creating a JsonRpc error response.
     * Wraps the error into a HandlerResult with appropriate response format.
     *
     * @param e The throwable that caused the error
     * @return Mono containing HandlerResult with JsonRpc error response
     */
    private Mono<HandlerResult> onMcpErrorResume(Throwable e) {
        return getMcpRequestId().map(id -> {
            JsonRpcResponse response = JsonRpcResponse.createServerErrorResponse(null, e.getMessage());
            MethodParameter parameter = new MethodParameter(McpToolMethod.HANDLE_METHOD, -1);
            return new HandlerResult(handler, response, parameter);
        });
    }

    /**
     * Extracts the request ID from MCP request body using JsonPath.
     *
     * @return A Mono containing the request ID, or empty if extraction fails
     */
    private Mono<Object> getMcpRequestId() {
        return DataBufferUtils.join(request.getBody())
                .map(dataBuffer -> {
                    byte[] bytes = new byte[dataBuffer.readableByteCount()];
                    dataBuffer.read(bytes);
                    DataBufferUtils.release(dataBuffer);
                    return new ByteArrayInputStream(bytes);
                })
                .map(is -> parser.read(is, JsonRpcRequest.JSON_PATH_ID))
                .onErrorResume(e -> null);
    }
}
