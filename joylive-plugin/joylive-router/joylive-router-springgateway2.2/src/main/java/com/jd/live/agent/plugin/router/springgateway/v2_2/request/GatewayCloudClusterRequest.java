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
package com.jd.live.agent.plugin.router.springgateway.v2_2.request;

import com.jd.live.agent.core.util.http.HttpMethod;
import com.jd.live.agent.governance.policy.service.cluster.RetryPolicy;
import com.jd.live.agent.plugin.router.springcloud.v2_2.request.AbstractCloudClusterRequest;
import com.jd.live.agent.plugin.router.springgateway.v2_2.cluster.context.GatewayClusterContext;
import com.jd.live.agent.plugin.router.springgateway.v2_2.config.GatewayConfig;
import lombok.Getter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpCookie;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.server.ServerWebExchange;

import java.net.URI;
import java.util.List;
import java.util.Map;

import static com.jd.live.agent.core.util.http.HttpUtils.parseCookie;
import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR;
import static org.springframework.http.HttpHeaders.writableHttpHeaders;

/**
 * GatewayOutboundRequest
 *
 * @since 1.0.0
 */
@Getter
public class GatewayCloudClusterRequest extends AbstractCloudClusterRequest<ServerHttpRequest, GatewayClusterContext> {

    private final ServerWebExchange exchange;

    private final GatewayFilterChain chain;

    private final GatewayConfig gatewayConfig;

    private final RetryPolicy retryPolicy;

    private final int index;

    public GatewayCloudClusterRequest(ServerWebExchange exchange,
                                      GatewayClusterContext context,
                                      GatewayFilterChain chain,
                                      GatewayConfig gatewayConfig,
                                      RetryPolicy retryPolicy,
                                      int index) {
        super(exchange.getRequest(), getURI(exchange), context);
        this.exchange = exchange;
        this.chain = chain;
        this.retryPolicy = retryPolicy;
        this.gatewayConfig = gatewayConfig;
        this.index = index;
        this.uri = exchange.getAttributeOrDefault(GATEWAY_REQUEST_URL_ATTR, exchange.getRequest().getURI());
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
        return key == null || key.isEmpty() ? null : request.getHeaders().getFirst(key);
    }

    @Override
    public void setHeader(String key, String value) {
        if (key != null && !key.isEmpty() && value != null && !value.isEmpty()) {
            writableHttpHeaders(request.getHeaders()).set(key, value);
        }
    }

    @Override
    public String getQuery(String key) {
        return key == null || key.isEmpty() ? null : request.getQueryParams().getFirst(key);
    }

    @Override
    public boolean isInstanceSensitive() {
        return context != null && context.isInstanceSensitive();
    }

    @Override
    protected Map<String, List<String>> parseCookies() {
        return parseCookie(request.getCookies(), HttpCookie::getValue);
    }

    @Override
    protected Map<String, List<String>> parseHeaders() {
        return request.getHeaders();
    }

    public RetryPolicy getDefaultRetryPolicy() {
        return retryPolicy;
    }

    private static URI getURI(ServerWebExchange exchange) {
        return exchange.getAttributeOrDefault(GATEWAY_REQUEST_URL_ATTR, exchange.getRequest().getURI());
    }

}
