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

import com.jd.live.agent.core.util.cache.UnsafeLazyObject;
import com.jd.live.agent.core.util.http.HttpMethod;
import com.jd.live.agent.core.util.http.HttpUtils;
import com.jd.live.agent.governance.request.AbstractHttpRequest.AbstractHttpInboundRequest;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.reactive.HandlerResult;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;
import java.util.concurrent.CompletableFuture;
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

    private final static Class<?> RESOURCE_HANDLER_CLASS = loadClass(RESOURCE_HANDLER_TYPE, ServerWebExchange.class.getClassLoader());

    private final Predicate<String> systemPredicate;

    private final Object handler;

    public ReactiveInboundRequest(ServerHttpRequest request, Object handler, Predicate<String> systemPredicate) {
        super(request);
        this.handler = handler;
        this.systemPredicate = systemPredicate;
        this.uri = request.getURI();
        this.headers = new UnsafeLazyObject<>(() -> HttpHeaders.writableHttpHeaders(request.getHeaders()));
        this.queries = new UnsafeLazyObject<>(() -> HttpUtils.parseQuery(request.getURI().getRawQuery()));
        this.cookies = new UnsafeLazyObject<>(() -> HttpUtils.parseCookie(request.getCookies(), HttpCookie::getValue));
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
        } else if (ACTUATOR_CLASS != null && ACTUATOR_CLASS.isInstance(handler)) {
            return true;
        } else if (systemPredicate != null && systemPredicate.test(getPath())) {
            return true;
        }
        return super.isSystem();
    }

    @Override
    public HttpMethod getHttpMethod() {
        org.springframework.http.HttpMethod method = request.getMethod();
        if (method == null) {
            return null;
        }
        try {
            // Compatible with spring web 6(class) & 5(enum).
            return HttpMethod.valueOf(method.name());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    @Override
    public String getCookie(String key) {
        HttpCookie cookie = key == null ? null : request.getCookies().getFirst(key);
        return cookie == null ? null : cookie.getValue();
    }

    /**
     * Converts a CompletionStage into a Mono that represents the completion of the stage.
     *
     * @param stage the CompletionStage to convert into a Mono.
     * @return a Mono that represents the completion of the stage.
     */
    public Mono<HandlerResult> convert(CompletionStage<Object> stage) {
        CompletableFuture<HandlerResult> future = new CompletableFuture<>();
        stage.whenComplete((r, t) -> {
            if (t != null) {
                future.completeExceptionally(THROWER.createException(t, this));
            } else if (r == null) {
                future.complete(null);
            } else if (r instanceof HandlerResult) {
                future.complete((HandlerResult) r);
            } else {
                future.completeExceptionally(new UnsupportedOperationException(
                        "Expected type is " + HandlerResult.class.getName() + ", but actual type is " + r.getClass()));
            }
        });
        return Mono.fromCompletionStage(future);
    }
}
