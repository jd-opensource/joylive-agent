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
package com.jd.live.agent.plugin.router.springgateway.v2_2.cluster;

import com.jd.live.agent.core.util.Futures;
import com.jd.live.agent.governance.context.RequestContext;
import com.jd.live.agent.governance.context.bag.Carrier;
import com.jd.live.agent.governance.context.bag.Propagation;
import com.jd.live.agent.governance.exception.ErrorPolicy;
import com.jd.live.agent.governance.exception.ErrorPredicate;
import com.jd.live.agent.governance.exception.ServiceError;
import com.jd.live.agent.governance.invoke.cluster.ClusterInvoker;
import com.jd.live.agent.governance.policy.service.circuitbreak.DegradeConfig;
import com.jd.live.agent.governance.policy.service.cluster.ClusterPolicy;
import com.jd.live.agent.governance.policy.service.cluster.RetryPolicy;
import com.jd.live.agent.governance.registry.ServiceEndpoint;
import com.jd.live.agent.governance.request.Request;
import com.jd.live.agent.plugin.router.springcloud.v2_2.cluster.AbstractCloudCluster;
import com.jd.live.agent.plugin.router.springgateway.v2_2.cluster.context.GatewayClusterContext;
import com.jd.live.agent.plugin.router.springgateway.v2_2.filter.LiveGatewayFilterChain;
import com.jd.live.agent.plugin.router.springgateway.v2_2.request.GatewayCloudClusterRequest;
import com.jd.live.agent.plugin.router.springgateway.v2_2.request.HttpHeadersParser;
import com.jd.live.agent.plugin.router.springgateway.v2_2.response.GatewayClusterResponse;
import lombok.Getter;
import org.reactivestreams.Publisher;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import org.springframework.lang.NonNull;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;

import static com.jd.live.agent.governance.instance.Endpoint.SECURE_SCHEME;
import static com.jd.live.agent.plugin.router.springcloud.v2_2.util.UriUtils.newURI;
import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.*;

@Getter
public class GatewayCluster extends AbstractCloudCluster<
        GatewayCloudClusterRequest,
        GatewayClusterResponse,
        GatewayClusterContext> {

    public GatewayCluster(GatewayClusterContext context) {
        super(context);
    }

    @Override
    public ClusterPolicy getDefaultPolicy(GatewayCloudClusterRequest request) {
        RetryPolicy retryPolicy = request.getDefaultRetryPolicy();
        return new ClusterPolicy(retryPolicy == null ? ClusterInvoker.TYPE_FAILFAST : ClusterInvoker.TYPE_FAILOVER, retryPolicy);
    }

    @Override
    public CompletionStage<GatewayClusterResponse> invoke(GatewayCloudClusterRequest request, ServiceEndpoint endpoint) {
        try {
            Set<ErrorPolicy> policies = request.removeErrorPolicies();
            // decorate request to transmission
            Propagation propagation = context.getPropagation();
            Carrier carrier = RequestContext.get();
            Consumer<ServerHttpRequest.Builder> header = b -> b.headers(
                    headers -> propagation.write(carrier, new HttpHeadersParser(headers)));
            ServerWebExchange.Builder builder = request.getExchange().mutate().request(header);
            // decorate response to remove exception header and get body
            BodyResponseDecorator decorator = new BodyResponseDecorator(request.getExchange(), policies);
            ServerWebExchange exchange = builder.response(decorator).build();
            GatewayClusterResponse response = new GatewayClusterResponse(exchange.getResponse(),
                    () -> (ServiceError) exchange.getAttributes().remove(Request.KEY_SERVER_ERROR),
                    () -> (String) exchange.getAttributes().remove(Request.KEY_RESPONSE_BODY));
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
    public void onStartRequest(GatewayCloudClusterRequest request, ServiceEndpoint endpoint) {
        if (endpoint != null) {
            ServerWebExchange exchange = request.getExchange();
            Map<String, Object> attributes = exchange.getAttributes();

            URI uri = exchange.getAttributeOrDefault(GATEWAY_REQUEST_URL_ATTR, request.getRequest().getURI());
            // preserve the original url
            Set<URI> urls = (Set<URI>) attributes.computeIfAbsent(GATEWAY_ORIGINAL_REQUEST_URL_ATTR, s -> new LinkedHashSet<>());
            urls.add(uri);

            // if the `lb:<scheme>` mechanism was used, use `<scheme>` as the default,
            // if the loadbalancer doesn't provide one.
            String overrideScheme = endpoint.isSecure() ? "https" : "http";
            String schemePrefix = (String) attributes.get(GATEWAY_SCHEME_PREFIX_ATTR);
            if (schemePrefix != null) {
                overrideScheme = request.getURI().getScheme();
            }

            boolean secure = SECURE_SCHEME.test(overrideScheme) || endpoint.isSecure();
            String scheme = endpoint.getScheme();
            scheme = scheme == null ? overrideScheme : scheme;
            URI requestUrl = newURI(uri, scheme, secure, endpoint.getHost(), endpoint.getPort());
            attributes.put(GATEWAY_REQUEST_URL_ATTR, requestUrl);
        }
        super.onStartRequest(request, endpoint);
    }

    @Override
    protected GatewayClusterResponse createResponse(GatewayCloudClusterRequest httpRequest, DegradeConfig degradeConfig) {
        return GatewayClusterResponse.create(httpRequest, degradeConfig);
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
            final HttpHeaders headers = exchange.getResponse().getHeaders();
            ServiceError error = ServiceError.build(key -> {
                List<String> values = headers.remove(key);
                return values == null || values.isEmpty() ? null : values.get(0);
            });
            if (error != null) {
                exchange.getAttributes().put(Request.KEY_SERVER_ERROR, error);
            }
            if (policies != null && !policies.isEmpty()) {
                String contentType = exchange.getAttribute(ORIGINAL_RESPONSE_CONTENT_TYPE_ATTR);
                if (body instanceof Mono && policyMatch(contentType)) {
                    Mono<? extends DataBuffer> monoBody = Mono.from(body);
                    return super.writeWith(monoBody.map(dataBuffer -> {
                        DataBufferFactory bufferFactory = bufferFactory();
                        byte[] data = new byte[dataBuffer.readableByteCount()];
                        dataBuffer.read(data);
                        // must release the buffer
                        DataBufferUtils.release(dataBuffer);
                        String res = new String(data, StandardCharsets.UTF_8);
                        exchange.getAttributes().put(Request.KEY_RESPONSE_BODY, res);
                        return bufferFactory.wrap(data);
                    }));
                }
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
            Integer status = getRawStatusCode();
            int ok = HttpStatus.OK.value();
            for (ErrorPolicy policy : policies) {
                if (policy.match(status, contentType, ok)) {
                    return true;
                }
            }
            return false;
        }
    }

}
