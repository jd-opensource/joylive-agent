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
import com.jd.live.agent.core.util.http.HttpUtils;
import com.jd.live.agent.governance.policy.service.cluster.RetryPolicy;
import com.jd.live.agent.plugin.router.springcloud.v2_2.request.AbstractCloudClusterRequest;
import com.jd.live.agent.plugin.router.springgateway.v2_2.cluster.context.GatewayClusterContext;
import com.jd.live.agent.plugin.router.springgateway.v2_2.config.GatewayConfig;
import lombok.Getter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.server.ServerWebExchange;

import java.net.URI;
import java.util.List;
import java.util.Map;

import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR;
import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR;

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

    private final HttpHeaders writeableHeaders;

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
        this.writeableHeaders = HttpHeaders.writableHttpHeaders(request.getHeaders());
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
            writeableHeaders.set(key, value);
        }
    }

    @Override
    public String getQuery(String key) {
        return key == null || key.isEmpty() ? null : request.getQueryParams().getFirst(key);
    }

    @Override
    public String getForwardHostExpression() {
        String result = null;
        if (context.getRegistryFactory() != null) {
            Route route = exchange.getAttribute(GATEWAY_ROUTE_ATTR);
            Map<String, Object> metadata = route == null ? null : route.getMetadata();
            result = metadata == null ? null : (String) metadata.get(GatewayConfig.KEY_HOST_EXPRESSION);
            result = result == null && gatewayConfig != null ? gatewayConfig.getHostExpression() : result;
        }
        return result;
    }

    @Override
    public void forward(String host) {
        exchange.getAttributes().put(GATEWAY_REQUEST_URL_ATTR, HttpUtils.newURI(uri, host));
    }

    @Override
    public boolean isInstanceSensitive() {
        return context.getRegistryFactory() != null;
    }

    @Override
    protected Map<String, List<String>> parseCookies() {
        return HttpUtils.parseCookie(request.getCookies(), HttpCookie::getValue);
    }

    @Override
    protected Map<String, List<String>> parseHeaders() {
        return writeableHeaders;
    }

    public RetryPolicy getDefaultRetryPolicy() {
        return retryPolicy;
    }

    private static URI getURI(ServerWebExchange exchange) {
        return exchange.getAttributeOrDefault(GATEWAY_REQUEST_URL_ATTR, exchange.getRequest().getURI());
    }

}
