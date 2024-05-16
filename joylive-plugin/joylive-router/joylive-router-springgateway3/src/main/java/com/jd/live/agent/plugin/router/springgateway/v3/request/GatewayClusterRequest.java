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
package com.jd.live.agent.plugin.router.springgateway.v3.request;

import com.jd.live.agent.core.util.cache.LazyObject;
import com.jd.live.agent.governance.request.HttpMethod;
import com.jd.live.agent.plugin.router.springcloud.v3.request.AbstractClusterRequest;
import lombok.Getter;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.RequestData;
import org.springframework.cloud.client.loadbalancer.reactive.ReactiveLoadBalancer;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.factory.RetryGatewayFilterFactory.RetryConfig;
import org.springframework.http.HttpCookie;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.server.ServerWebExchange;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR;

/**
 * GatewayOutboundRequest
 *
 * @since 1.0.0
 */
@Getter
public class GatewayClusterRequest extends AbstractClusterRequest<ServerHttpRequest> {

    private final ServerWebExchange exchange;

    private final GatewayFilterChain chain;

    private final RetryConfig retryConfig;

    public GatewayClusterRequest(ServerWebExchange exchange,
                                 GatewayFilterChain chain,
                                 ReactiveLoadBalancer.Factory<ServiceInstance> factory,
                                 RetryConfig retryConfig) {
        super(exchange.getRequest(), factory);
        this.exchange = exchange;
        this.chain = chain;
        this.uri = exchange.getAttribute(GATEWAY_REQUEST_URL_ATTR);
        this.queries = new LazyObject<>(() -> parseQuery(request.getURI().getQuery()));
        this.headers = new LazyObject<>(request.getHeaders());
        this.cookies = new LazyObject<>(() -> parseCookie(request));
        this.retryConfig = retryConfig;

    }

    @Override
    public HttpMethod getHttpMethod() {
        org.springframework.http.HttpMethod method = request.getMethod();
        try {
            return method == null ? null : HttpMethod.valueOf(method.name());
        } catch (IllegalArgumentException ignore) {
            return null;
        }
    }

    @Override
    public String getCookie(String key) {
        HttpCookie cookie = request.getCookies().getFirst(key);
        return cookie == null ? null : cookie.getValue();
    }

    @Override
    protected RequestData buildRequestData() {
        return new RequestData(request);
    }

    protected Map<String, List<String>> parseCookie(ServerHttpRequest request) {
        Map<String, List<String>> result = new HashMap<>();
        request.getCookies().forEach((n, v) -> result.put(n,
                v.stream().map(HttpCookie::getValue).collect(Collectors.toList())));
        return result;
    }
}
