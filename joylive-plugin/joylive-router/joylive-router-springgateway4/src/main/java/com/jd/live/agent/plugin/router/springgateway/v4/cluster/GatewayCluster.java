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
package com.jd.live.agent.plugin.router.springgateway.v4.cluster;

import com.jd.live.agent.core.Constants;
import com.jd.live.agent.core.util.Futures;
import com.jd.live.agent.governance.exception.ErrorPolicy;
import com.jd.live.agent.governance.exception.ErrorPredicate;
import com.jd.live.agent.governance.exception.ServiceError;
import com.jd.live.agent.governance.invoke.cluster.ClusterInvoker;
import com.jd.live.agent.governance.policy.service.circuitbreak.DegradeConfig;
import com.jd.live.agent.governance.policy.service.cluster.ClusterPolicy;
import com.jd.live.agent.governance.policy.service.cluster.RetryPolicy;
import com.jd.live.agent.governance.policy.service.exception.CodePolicy;
import com.jd.live.agent.governance.request.Request;
import com.jd.live.agent.plugin.router.springcloud.v3.cluster.AbstractClientCluster;
import com.jd.live.agent.plugin.router.springcloud.v3.instance.SpringEndpoint;
import com.jd.live.agent.plugin.router.springgateway.v4.filter.LiveGatewayFilterChain;
import com.jd.live.agent.plugin.router.springgateway.v4.request.GatewayClusterRequest;
import com.jd.live.agent.plugin.router.springgateway.v4.response.GatewayClusterResponse;
import lombok.Getter;
import org.reactivestreams.Publisher;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.CompletionContext;
import org.springframework.cloud.client.loadbalancer.RequestData;
import org.springframework.cloud.client.loadbalancer.ResponseData;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.factory.RetryGatewayFilterFactory;
import org.springframework.cloud.gateway.filter.factory.RetryGatewayFilterFactory.RetryConfig;
import org.springframework.cloud.gateway.support.DelegatingServiceInstance;
import org.springframework.cloud.loadbalancer.support.LoadBalancerClientFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import org.springframework.lang.NonNull;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CompletionStage;

import static com.jd.live.agent.core.util.StringUtils.split;
import static java.util.Arrays.asList;
import static org.springframework.cloud.client.loadbalancer.LoadBalancerUriTools.reconstructURI;
import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.*;

@Getter
public class GatewayCluster extends AbstractClientCluster<GatewayClusterRequest, GatewayClusterResponse> {

    private final LoadBalancerClientFactory clientFactory;

    public GatewayCluster(LoadBalancerClientFactory clientFactory) {
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
    protected boolean isRetryable() {
        return true;
    }

    @Override
    public CompletionStage<GatewayClusterResponse> invoke(GatewayClusterRequest request, SpringEndpoint endpoint) {
        try {
            Set<ErrorPolicy> policies = request.getAttribute(Request.KEY_ERROR_POLICY);
            BodyResponseDecorator decorator = new BodyResponseDecorator(request.getExchange(), policies);
            ServerWebExchange exchange = request.getExchange().mutate().response(decorator).build();
            GatewayClusterResponse response = new GatewayClusterResponse(exchange.getResponse(),
                    () -> (ServiceError) exchange.getAttributes().get(Request.KEY_SERVER_ERROR),
                    () -> (String) exchange.getAttributes().get(Request.KEY_RESPONSE_BODY));
            GatewayFilterChain chain = request.getChain();
            if (chain instanceof LiveGatewayFilterChain) {
                // for retry
                ((LiveGatewayFilterChain) chain).setIndex(request.getIndex() + 1);
            }
            return chain.filter(exchange).toFuture().thenApply(v -> response);
        } catch (Throwable e) {
            return Futures.future(e);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onStartRequest(GatewayClusterRequest request, SpringEndpoint endpoint) {
        if (endpoint != null) {
            ServiceInstance instance = endpoint.getInstance();
            ServerWebExchange exchange = request.getExchange();
            Map<String, Object> attributes = exchange.getAttributes();

            URI uri = exchange.getAttributeOrDefault(GATEWAY_REQUEST_URL_ATTR, request.getRequest().getURI());
            // preserve the original url
            Set<URI> urls = (Set<URI>) attributes.computeIfAbsent(GATEWAY_ORIGINAL_REQUEST_URL_ATTR, s -> new LinkedHashSet<>());
            urls.add(uri);

            // if the `lb:<scheme>` mechanism was used, use `<scheme>` as the default,
            // if the loadbalancer doesn't provide one.
            String overrideScheme = instance.isSecure() ? "https" : "http";

            String schemePrefix = (String) attributes.get(GATEWAY_SCHEME_PREFIX_ATTR);
            if (schemePrefix != null) {
                overrideScheme = request.getURI().getScheme();
            }
            URI requestUrl = reconstructURI(new DelegatingServiceInstance(instance, overrideScheme), uri);

            attributes.put(GATEWAY_REQUEST_URL_ATTR, requestUrl);
            attributes.put(GATEWAY_LOADBALANCER_RESPONSE_ATTR, endpoint.getResponse());
        }
        super.onStartRequest(request, endpoint);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onSuccess(GatewayClusterResponse response, GatewayClusterRequest request, SpringEndpoint endpoint) {
        ServerWebExchange exchange = request.getExchange();
        request.lifecycles(l -> l.onComplete(new CompletionContext<>(
                CompletionContext.Status.SUCCESS,
                request.getLbRequest(),
                endpoint.getResponse(),
                new ResponseData(exchange.getResponse(),
                        new RequestData(exchange.getRequest(), exchange.getAttributes())))));
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
            handleError();

            if (policies != null && !policies.isEmpty()) {
                String contentType = exchange.getAttribute(ORIGINAL_RESPONSE_CONTENT_TYPE_ATTR);
                if (body instanceof Flux && policyMatch(contentType)) {
                    Flux<? extends DataBuffer> fluxBody = Flux.from(body);
                    return super.writeWith(fluxBody.buffer().map(dataBuffers -> {
                        DataBufferFactory bufferFactory = bufferFactory();
                        DataBuffer join = bufferFactory.join(dataBuffers);
                        byte[] content = new byte[join.readableByteCount()];
                        join.read(content);
                        DataBufferUtils.release(join);
                        exchange.getAttributes().put(Request.KEY_RESPONSE_BODY, new String(content, StandardCharsets.UTF_8));
                        return bufferFactory.wrap(content);
                    }));
                }
            }
            return super.writeWith(body);
        }

        /**
         * Handles any errors that occurred during the processing of the request.
         *
         * @return true if an error was handled, false otherwise
         */
        protected boolean handleError() {
            HttpHeaders headers = exchange.getResponse().getHeaders();
            List<String> exceptionMessages = headers.remove(Constants.EXCEPTION_MESSAGE_LABEL);
            String exceptionMessage = exceptionMessages != null && !exceptionMessages.isEmpty() ? exceptionMessages.get(0) : null;
            try {
                exceptionMessage = exceptionMessage == null || exceptionMessage.isEmpty()
                        ? exceptionMessage
                        : URLDecoder.decode(exceptionMessage, StandardCharsets.UTF_8.name());
            } catch (UnsupportedEncodingException ignore) {
            }
            List<String> exceptionNames = headers.remove(Constants.EXCEPTION_NAMES_LABEL);
            String exceptionName = exceptionNames != null && !exceptionNames.isEmpty() ? exceptionNames.get(0) : null;
            Set<String> exceptionNamesSet = exceptionName == null || exceptionName.isEmpty() ? null : new LinkedHashSet<>(asList(split(exceptionName)));
            if (exceptionMessage != null && !exceptionMessage.isEmpty() || exceptionNamesSet != null && !exceptionNamesSet.isEmpty()) {
                ServiceError error = new ServiceError(exceptionMessage, exceptionNamesSet, true);
                exchange.getAttributes().put(Request.KEY_SERVER_ERROR, error);
                return true;
            }
            return false;
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
            HttpStatusCode statusCode = getStatusCode();
            Integer status = statusCode == null ? null : statusCode.value();
            for (ErrorPolicy policy : policies) {
                codePolicy = policy.getCodePolicy();
                if (codePolicy != null && codePolicy.match(status, contentType, HttpStatus.OK.value())) {
                    return true;
                }
            }
            return false;
        }
    }
}
