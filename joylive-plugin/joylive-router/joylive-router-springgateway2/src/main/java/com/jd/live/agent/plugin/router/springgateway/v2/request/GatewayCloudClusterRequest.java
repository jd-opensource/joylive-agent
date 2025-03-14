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

import com.jd.live.agent.core.util.http.HttpMethod;
import com.jd.live.agent.core.util.http.HttpUtils;
import com.jd.live.agent.governance.policy.service.cluster.RetryPolicy;
import com.jd.live.agent.plugin.router.springcloud.v2.request.AbstractCloudClusterRequest;
import com.jd.live.agent.plugin.router.springgateway.v2.cluster.context.GatewayClusterContext;
import com.jd.live.agent.plugin.router.springgateway.v2.config.GatewayConfig;
import lombok.Getter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.factory.RetryGatewayFilterFactory;
import org.springframework.cloud.gateway.filter.factory.RetryGatewayFilterFactory.RetryConfig;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.server.ServerWebExchange;

import java.net.URI;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

    private final RetryConfig retryConfig;

    private final int index;

    private final HttpHeaders writeableHeaders;

    public GatewayCloudClusterRequest(ServerWebExchange exchange,
                                      GatewayClusterContext context,
                                      GatewayFilterChain chain,
                                      GatewayConfig gatewayConfig,
                                      RetryConfig retryConfig,
                                      int index) {
        super(exchange.getRequest(), getURI(exchange), context);
        this.exchange = exchange;
        this.chain = chain;
        this.retryConfig = retryConfig;
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
        if (context.getLoadBalancerFactory() != null) {
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
        return context.getLoadBalancerFactory() != null;
    }

    @Override
    protected Map<String, List<String>> parseCookies() {
        return HttpUtils.parseCookie(request.getCookies(), HttpCookie::getValue);
    }

    @Override
    protected Map<String, List<String>> parseHeaders() {
        return writeableHeaders;
    }

    @Override
    public RetryPolicy getDefaultRetryPolicy() {
        RetryConfig retryConfig = getRetryConfig();
        if (retryConfig != null && retryConfig.getRetries() > 0) {
            List<org.springframework.http.HttpMethod> methods = retryConfig.getMethods();
            if (methods.isEmpty() || methods.contains(request.getMethod())) {
                RetryGatewayFilterFactory.BackoffConfig backoff = retryConfig.getBackoff();
                Set<String> statuses = new HashSet<>(16);
                retryConfig.getStatuses().forEach(status -> statuses.add(String.valueOf(status.value())));
                Set<HttpStatus.Series> series = new HashSet<>(retryConfig.getSeries());
                if (!series.isEmpty()) {
                    for (HttpStatus status : HttpStatus.values()) {
                        if (series.contains(status.series())) {
                            statuses.add(String.valueOf(status.value()));
                        }
                    }
                }
                Set<String> exceptions = new HashSet<>();
                retryConfig.getExceptions().forEach(e -> exceptions.add(e.getName()));

                RetryPolicy retryPolicy = new RetryPolicy();
                retryPolicy.setRetry(retryConfig.getRetries());
                retryPolicy.setInterval(backoff != null ? backoff.getFirstBackoff().toMillis() : null);
                retryPolicy.setErrorCodes(statuses);
                retryPolicy.setExceptions(exceptions);
                return retryPolicy;
            }
        }
        return null;
    }

    private static URI getURI(ServerWebExchange exchange) {
        return exchange.getAttributeOrDefault(GATEWAY_REQUEST_URL_ATTR, exchange.getRequest().getURI());
    }

}
