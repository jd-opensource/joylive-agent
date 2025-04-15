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

import com.jd.live.agent.core.util.Futures;
import com.jd.live.agent.governance.exception.ErrorPolicy;
import com.jd.live.agent.governance.exception.ErrorPredicate;
import com.jd.live.agent.governance.exception.ServiceError;
import com.jd.live.agent.governance.policy.service.circuitbreak.DegradeConfig;
import com.jd.live.agent.governance.registry.Registry;
import com.jd.live.agent.governance.registry.ServiceEndpoint;
import com.jd.live.agent.governance.request.Request;
import com.jd.live.agent.plugin.router.springcloud.v4.cluster.AbstractCloudCluster;
import com.jd.live.agent.plugin.router.springgateway.v4.cluster.context.GatewayClusterContext;
import com.jd.live.agent.plugin.router.springgateway.v4.filter.LiveGatewayFilterChain;
import com.jd.live.agent.plugin.router.springgateway.v4.request.GatewayCloudClusterRequest;
import com.jd.live.agent.plugin.router.springgateway.v4.response.GatewayClusterResponse;
import lombok.Getter;
import org.reactivestreams.Publisher;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.reactive.ReactiveLoadBalancer;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import org.springframework.lang.NonNull;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletionStage;

import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.ORIGINAL_RESPONSE_CONTENT_TYPE_ATTR;

@Getter
public class GatewayCluster extends AbstractCloudCluster<
        GatewayCloudClusterRequest,
        GatewayClusterResponse,
        GatewayClusterContext> {

    public GatewayCluster(Registry registry, ReactiveLoadBalancer.Factory<ServiceInstance> clientFactory) {
        super(new GatewayClusterContext(registry, clientFactory));
    }

    @Override
    public CompletionStage<GatewayClusterResponse> invoke(GatewayCloudClusterRequest request, ServiceEndpoint endpoint) {
        try {
            Set<ErrorPolicy> policies = request.removeErrorPolicies();
            // decorate response to remove exception header and get body
            BodyResponseDecorator decorator = new BodyResponseDecorator(request.getExchange(), policies);
            ServerWebExchange exchange = request.getExchange().mutate().response(decorator).build();
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

    @Override
    public void onSuccess(GatewayClusterResponse response, GatewayCloudClusterRequest request, ServiceEndpoint endpoint) {
        request.onSuccess(response, endpoint);
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
                if (body instanceof Flux && policyMatch(contentType)) {
                    Flux<? extends DataBuffer> fluxBody = Flux.from(body);
                    return super.writeWith(fluxBody.buffer().map(dataBuffers -> {
                        DataBufferFactory bufferFactory = bufferFactory();
                        DataBuffer join = bufferFactory.join(dataBuffers);
                        byte[] content = new byte[join.readableByteCount()];
                        join.read(content);
                        // must release the buffer
                        DataBufferUtils.release(join);
                        exchange.getAttributes().put(Request.KEY_RESPONSE_BODY, new String(content, StandardCharsets.UTF_8));
                        return bufferFactory.wrap(content);
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
            HttpStatusCode statusCode = getStatusCode();
            Integer status = statusCode == null ? null : statusCode.value();
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
