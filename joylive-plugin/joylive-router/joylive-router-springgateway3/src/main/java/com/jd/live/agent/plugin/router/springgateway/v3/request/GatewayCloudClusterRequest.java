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

import com.jd.live.agent.core.util.http.HttpMethod;
import com.jd.live.agent.core.util.http.HttpUtils;
import com.jd.live.agent.governance.policy.service.cluster.RetryPolicy;
import com.jd.live.agent.governance.registry.ServiceEndpoint;
import com.jd.live.agent.plugin.router.springcloud.v3.request.AbstractCloudClusterRequest;
import com.jd.live.agent.plugin.router.springcloud.v3.response.SpringClusterResponse;
import com.jd.live.agent.plugin.router.springgateway.v3.cluster.context.GatewayClusterContext;
import com.jd.live.agent.plugin.router.springgateway.v3.config.GatewayConfig;
import com.jd.live.agent.plugin.router.springgateway.v3.response.GatewayClusterResponse;
import com.jd.live.agent.plugin.router.springgateway.v3.util.WebExchangeUtils;
import lombok.Getter;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.CompletionContext;
import org.springframework.cloud.client.loadbalancer.CompletionContext.Status;
import org.springframework.cloud.client.loadbalancer.RequestData;
import org.springframework.cloud.client.loadbalancer.ResponseData;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.factory.RetryGatewayFilterFactory;
import org.springframework.cloud.gateway.filter.factory.RetryGatewayFilterFactory.RetryConfig;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.server.ServerWebExchange;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.jd.live.agent.plugin.router.springcloud.v3.instance.SpringEndpoint.getResponse;
import static com.jd.live.agent.plugin.router.springgateway.v3.util.WebExchangeUtils.forward;

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
        super(exchange.getRequest(), WebExchangeUtils.getURI(exchange), context);
        this.exchange = exchange;
        this.chain = chain;
        this.retryConfig = retryConfig;
        this.gatewayConfig = gatewayConfig;
        this.index = index;
        this.writeableHeaders = HttpHeaders.writableHttpHeaders(request.getHeaders());
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
            writeableHeaders.set(key, value);
        }
    }

    @Override
    public String getQuery(String key) {
        return key == null || key.isEmpty() ? null : request.getQueryParams().getFirst(key);
    }

    @Override
    public boolean isInstanceSensitive() {
        return context.getLoadBalancerFactory() != null;
    }

    @Override
    protected RequestData buildRequestData() {
        // cookie is used only in RequestBasedStickySessionServiceInstanceListSupplier
        // it's disabled by live interceptor
        // so we can use null value to improve performance.
        return new RequestData(request.getMethod(), request.getURI(), request.getHeaders(), null, null);
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

    @Override
    public void onStartRequest(ServiceEndpoint endpoint) {
        if (endpoint != null) {
            forward(exchange, endpoint);
        }
        super.onStartRequest(endpoint);
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onSuccess(SpringClusterResponse response, ServiceEndpoint endpoint) {
        GatewayClusterResponse gcr = (GatewayClusterResponse) response;
        boolean useRawStatusCodeInResponseData = serviceContext.isUseRawStatusCodeInResponseData();
        ResponseData responseData = useRawStatusCodeInResponseData
                ? new ResponseData(new RequestData(request), gcr.getResponse())
                : new ResponseData(gcr.getResponse(), new RequestData(request));

        CompletionContext<ResponseData, ServiceInstance, ?> ctx = new CompletionContext<>(Status.SUCCESS, lbRequest, getResponse(endpoint), responseData);
        lifecycle(l -> l.onComplete(ctx));
    }

}
