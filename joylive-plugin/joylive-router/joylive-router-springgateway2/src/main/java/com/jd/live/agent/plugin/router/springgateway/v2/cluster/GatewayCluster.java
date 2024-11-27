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
package com.jd.live.agent.plugin.router.springgateway.v2.cluster;

import com.jd.live.agent.core.util.Futures;
import com.jd.live.agent.governance.context.RequestContext;
import com.jd.live.agent.governance.exception.ErrorPolicy;
import com.jd.live.agent.governance.exception.ErrorPredicate;
import com.jd.live.agent.governance.exception.ServiceError;
import com.jd.live.agent.governance.invoke.cluster.ClusterInvoker;
import com.jd.live.agent.governance.policy.service.circuitbreak.DegradeConfig;
import com.jd.live.agent.governance.policy.service.cluster.ClusterPolicy;
import com.jd.live.agent.governance.policy.service.cluster.RetryPolicy;
import com.jd.live.agent.governance.policy.service.exception.CodePolicy;
import com.jd.live.agent.governance.request.Request;
import com.jd.live.agent.plugin.router.springcloud.v2.cluster.AbstractClientCluster;
import com.jd.live.agent.plugin.router.springcloud.v2.instance.SpringEndpoint;
import com.jd.live.agent.plugin.router.springgateway.v2.request.GatewayClusterRequest;
import com.jd.live.agent.plugin.router.springgateway.v2.response.GatewayClusterResponse;
import lombok.Getter;
import org.reactivestreams.Publisher;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.reactive.ReactiveLoadBalancer;
import org.springframework.cloud.gateway.filter.factory.RetryGatewayFilterFactory;
import org.springframework.cloud.gateway.filter.factory.RetryGatewayFilterFactory.RetryConfig;
import org.springframework.cloud.gateway.support.DelegatingServiceInstance;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import org.springframework.lang.NonNull;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletionStage;

import static org.springframework.cloud.client.loadbalancer.LoadBalancerUriTools.reconstructURI;
import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.*;

@Getter
public class GatewayCluster extends AbstractClientCluster<GatewayClusterRequest, GatewayClusterResponse> {

    private final ReactiveLoadBalancer.Factory<ServiceInstance> clientFactory;

    public GatewayCluster(ReactiveLoadBalancer.Factory<ServiceInstance> clientFactory) {
        this.clientFactory = clientFactory;
    }

    @Override
    public ClusterPolicy getDefaultPolicy(GatewayClusterRequest request) {
        RetryConfig retryConfig = request.getRetryConfig();
        if (retryConfig != null && retryConfig.getRetries() > 0) {
            List<HttpMethod> methods = retryConfig.getMethods();
            if (methods.isEmpty() || methods.contains(request.getRequest().getMethod())) {
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
                return new ClusterPolicy(ClusterInvoker.TYPE_FAILOVER, retryPolicy);
            }
        }
        return new ClusterPolicy(ClusterInvoker.TYPE_FAILFAST);
    }

    @Override
    public CompletionStage<GatewayClusterResponse> invoke(GatewayClusterRequest request, SpringEndpoint endpoint) {
        try {
            ServerWebExchange exchange = getWebExchange(request);
            GatewayClusterResponse response = new GatewayClusterResponse(exchange.getResponse(), () -> (String) exchange.getAttributes().get(Request.KEY_RESPONSE_BODY));
            return request.getChain().filter(exchange).toFuture().thenApply(v -> response);
        } catch (Throwable e) {
            return Futures.future(e);
        }
    }

    @Override
    public void onStartRequest(GatewayClusterRequest request, SpringEndpoint endpoint) {
        if (endpoint != null) {
            ServiceInstance instance = endpoint.getInstance();
            ServerWebExchange exchange = request.getExchange();
            URI uri = exchange.getAttributeOrDefault(GATEWAY_REQUEST_URL_ATTR, request.getRequest().getURI());
            // if the `lb:<scheme>` mechanism was used, use `<scheme>` as the default,
            // if the loadbalancer doesn't provide one.
            String overrideScheme = instance.isSecure() ? "https" : "http";
            Map<String, Object> attributes = exchange.getAttributes();
            String schemePrefix = (String) attributes.get(GATEWAY_SCHEME_PREFIX_ATTR);
            if (schemePrefix != null) {
                overrideScheme = request.getURI().getScheme();
            }
            URI requestUrl = reconstructURI(new DelegatingServiceInstance(instance, overrideScheme), uri);

            attributes.put(GATEWAY_REQUEST_URL_ATTR, requestUrl);
        }
        super.onStartRequest(request, endpoint);
    }

    @Override
    protected GatewayClusterResponse createResponse(GatewayClusterRequest httpRequest, DegradeConfig degradeConfig) {
        ServerHttpResponse response = httpRequest.getExchange().getResponse();
        ServerHttpRequest request = httpRequest.getExchange().getRequest();

        String body = degradeConfig.getResponseBody();
        int length = body == null ? 0 : body.length();
        byte[] bytes = length == 0 ? new byte[0] : body.getBytes(StandardCharsets.UTF_8);
        DataBuffer buffer = response.bufferFactory().wrap(bytes);

        HttpHeaders headers = HttpHeaders.writableHttpHeaders(response.getHeaders());
        headers.putAll(request.getHeaders());
        Map<String, String> attributes = degradeConfig.getAttributes();
        if (attributes != null) {
            attributes.forEach(headers::add);
        }
        response.setRawStatusCode(degradeConfig.getResponseCode());
        response.setStatusCode(HttpStatus.valueOf(degradeConfig.getResponseCode()));
        headers.set(HttpHeaders.CONTENT_TYPE, degradeConfig.contentType());

        response.writeWith(Flux.just(buffer)).subscribe();
        return new GatewayClusterResponse(response);
    }

    @Override
    protected GatewayClusterResponse createResponse(ServiceError error, ErrorPredicate predicate) {
        return new GatewayClusterResponse(error, predicate);
    }

    /**
     * Returns a mutated ServerWebExchange based on the given GatewayClusterRequest.
     *
     * @param request The GatewayClusterRequest to process
     * @return The mutated ServerWebExchange
     */
    private ServerWebExchange getWebExchange(GatewayClusterRequest request) {
        Set<ErrorPolicy> policies = request.getAttribute(Request.KEY_ERROR_POLICY);
        ServerWebExchange.Builder builder = null;
        if (policies != null && !policies.isEmpty() || RequestContext.hasCargo()) {
            builder = request.getExchange().mutate();
            if (policies != null && !policies.isEmpty()) {
                builder = builder.response(new BodyResponseDecorator(request.getExchange(), policies));
            }
            if (RequestContext.hasCargo()) {
                builder = builder.request(b -> b.headers(headers -> RequestContext.cargos(headers::set)));
            }
        }
        return builder == null ? request.getExchange() : builder.build();
    }

    /**
     * A decorator for {@link ServerHttpResponse} that modifies the response body according to the specified code policies.
     */
    private static class BodyResponseDecorator extends ServerHttpResponseDecorator {

        private final ServerWebExchange exchange;

        private final Set<ErrorPolicy> policies;

        BodyResponseDecorator(ServerWebExchange exchange, Set<ErrorPolicy> policies) {
            super(exchange.getResponse());
            this.exchange = exchange;
            this.policies = policies;
        }

        @NonNull
        @Override
        public Mono<Void> writeWith(@NonNull Publisher<? extends DataBuffer> body) {
            String contentType = exchange.getAttribute(ORIGINAL_RESPONSE_CONTENT_TYPE_ATTR);
            contentType = contentType != null ? contentType : getHeaders().getFirst(HttpHeaders.CONTENT_TYPE);
            if (body instanceof Mono && policyMatch(contentType)) {
                Mono<? extends DataBuffer> monoBody = Mono.from(body);
                return super.writeWith(monoBody.map(dataBuffer -> {
                    DataBufferFactory bufferFactory = bufferFactory();
                    byte[] data = new byte[dataBuffer.readableByteCount()];
                    dataBuffer.read(data);
                    DataBufferUtils.release(dataBuffer);
                    String res = new String(data, StandardCharsets.UTF_8);
                    exchange.getAttributes().put(Request.KEY_RESPONSE_BODY, res);
                    return bufferFactory.wrap(data);
                }));
            }
            return super.writeWith(body);
        }

        /**
         * Checks if any of the code policies match the given content type.
         *
         * @param contentType The content type to check.
         * @return true if any of the code policies match the content type, false otherwise.
         */
        private boolean policyMatch(String contentType) {
            contentType = contentType == null ? null : contentType.toLowerCase();
            CodePolicy codePolicy;
            for (ErrorPolicy policy : policies) {
                codePolicy = policy.getCodePolicy();
                if (codePolicy != null && codePolicy.match(getRawStatusCode(), contentType, HttpStatus.OK.value())) {
                    return true;
                }
            }
            return false;
        }
    }

}
