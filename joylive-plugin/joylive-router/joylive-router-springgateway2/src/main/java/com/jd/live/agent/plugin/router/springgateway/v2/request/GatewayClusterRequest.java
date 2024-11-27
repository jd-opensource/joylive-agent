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
package com.jd.live.agent.plugin.router.springgateway.v2.request;

import com.jd.live.agent.core.util.cache.UnsafeLazyObject;
import com.jd.live.agent.core.util.http.HttpMethod;
import com.jd.live.agent.core.util.http.HttpUtils;
import com.jd.live.agent.governance.exception.ErrorPolicy;
import com.jd.live.agent.plugin.router.springcloud.v2.request.AbstractClusterRequest;
import com.jd.live.agent.plugin.router.springgateway.v2.config.GatewayConfig;
import lombok.Getter;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.reactive.ReactiveLoadBalancer;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.factory.RetryGatewayFilterFactory.RetryConfig;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR;
import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR;

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

    private final GatewayConfig gatewayConfig;

    public GatewayClusterRequest(ServerWebExchange exchange,
                                 GatewayFilterChain chain,
                                 ReactiveLoadBalancer.Factory<ServiceInstance> factory,
                                 RetryConfig retryConfig,
                                 GatewayConfig gatewayConfig) {
        super(exchange.getRequest(), factory);
        this.exchange = exchange;
        this.uri = exchange.getAttributeOrDefault(GATEWAY_REQUEST_URL_ATTR, exchange.getRequest().getURI());
        this.chain = chain;
        this.queries = new UnsafeLazyObject<>(() -> HttpUtils.parseQuery(request.getURI().getRawQuery()));
        this.headers = new UnsafeLazyObject<>(() -> HttpHeaders.writableHttpHeaders(request.getHeaders()));
        this.cookies = new UnsafeLazyObject<>(() -> HttpUtils.parseCookie(request.getCookies(), HttpCookie::getValue));
        this.retryConfig = retryConfig;
        this.gatewayConfig = gatewayConfig;
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
    public String getForwardHostExpression() {
        String result = null;
        if (loadBalancerFactory != null) {
            Route route = exchange.getAttribute(GATEWAY_ROUTE_ATTR);
            Map<String, Object> metadata = route == null ? null : route.getMetadata();
            result = metadata == null ? null : (String) metadata.get(GatewayConfig.KEY_HOST_EXPRESSION);
            result = result == null && gatewayConfig != null ? gatewayConfig.getHostExpression() : result;
        }
        return result;
    }

    @Override
    public void forward(String host) {
        exchange.getAttributes().put(GATEWAY_REQUEST_URL_ATTR, UriComponentsBuilder.fromUri(uri).host(host).build().toUri());
    }

    @Override
    public boolean isInstanceSensitive() {
        return loadBalancerFactory != null;
    }

    @Override
    public boolean requireResponseBody(ErrorPolicy policy) {
        return policy.getCodePolicy() != null && policy.getCodePolicy().isBodyRequest()
                || policy.getExceptions() != null && !policy.getExceptions().isEmpty();
    }
}
