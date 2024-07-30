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
package com.jd.live.agent.plugin.router.springcloud.v3.request;

import com.jd.live.agent.core.util.cache.UnsafeLazyObject;
import com.jd.live.agent.core.util.http.HttpMethod;
import com.jd.live.agent.core.util.http.HttpUtils;
import com.jd.live.agent.governance.request.AbstractHttpRequest.AbstractHttpInboundRequest;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.server.ServerWebExchange;

import java.net.InetSocketAddress;
import java.util.function.Predicate;

import static com.jd.live.agent.core.util.type.ClassUtils.loadClass;

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
        switch (method) {
            case GET:
                return HttpMethod.GET;
            case PUT:
                return HttpMethod.PUT;
            case POST:
                return HttpMethod.POST;
            case DELETE:
                return HttpMethod.DELETE;
            case HEAD:
                return HttpMethod.HEAD;
            case PATCH:
                return HttpMethod.PATCH;
            case OPTIONS:
                return HttpMethod.OPTIONS;
            case TRACE:
                return HttpMethod.TRACE;
            default:
                return null;
        }
    }

    @Override
    public String getCookie(String key) {
        String result = null;
        if (key != null) {
            HttpCookie cookie = request.getCookies().getFirst(key);
            result = cookie == null ? null : cookie.getValue();
        }
        return result;
    }
}
