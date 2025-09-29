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
package com.jd.live.agent.plugin.router.springgateway.v3.cluster;

import com.jd.live.agent.core.util.Futures;
import com.jd.live.agent.governance.exception.ErrorPolicy;
import com.jd.live.agent.governance.exception.ErrorPredicate;
import com.jd.live.agent.governance.exception.ServiceError;
import com.jd.live.agent.governance.policy.service.circuitbreak.DegradeConfig;
import com.jd.live.agent.governance.registry.Registry;
import com.jd.live.agent.governance.registry.ServiceEndpoint;
import com.jd.live.agent.governance.request.Request;
import com.jd.live.agent.plugin.router.springcloud.v3.cluster.AbstractCloudCluster;
import com.jd.live.agent.plugin.router.springgateway.v3.cluster.context.GatewayClusterContext;
import com.jd.live.agent.plugin.router.springgateway.v3.filter.LiveGatewayFilterChain;
import com.jd.live.agent.plugin.router.springgateway.v3.request.GatewayCloudClusterRequest;
import com.jd.live.agent.plugin.router.springgateway.v3.response.ErrorResponseDecorator;
import com.jd.live.agent.plugin.router.springgateway.v3.response.GatewayClusterResponse;
import com.jd.live.agent.plugin.router.springgateway.v3.util.WebExchangeUtils;
import lombok.Getter;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.reactive.ReactiveLoadBalancer;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.netty.Connection;

import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Supplier;

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
            Set<ErrorPolicy> policies = request.getErrorPolicies();
            // decorate response to remove exception header and get body
            boolean failover = request.getAttributeOrDefault(Request.KEY_FAILOVER_REQUEST, Boolean.FALSE);
            ErrorResponseDecorator decorator = new ErrorResponseDecorator(request.getExchange(), policies, failover);
            ServerWebExchange exchange = request.getExchange().mutate().response(decorator).build();
            GatewayClusterResponse response = new GatewayClusterResponse(exchange);
            GatewayFilterChain chain = request.getChain();
            if (chain instanceof LiveGatewayFilterChain) {
                // for failover
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

    @Override
    public void onRetry(GatewayCloudClusterRequest request, int retries) {
        if (retries > 0) {
            ServerWebExchange exchange = request.getExchange();
            WebExchangeUtils.removeAttribute(exchange, Request.KEY_RESPONSE_BODY);
            WebExchangeUtils.removeAttribute(exchange, Request.KEY_RESPONSE_WRITE);
            Connection conn = WebExchangeUtils.removeAttribute(exchange, ServerWebExchangeUtils.CLIENT_RESPONSE_CONN_ATTR);
            if (conn != null) {
                conn.dispose();
            }
            WebExchangeUtils.reset(exchange);
        }
    }

    @Override
    public void onRetryComplete(CompletableFuture<GatewayClusterResponse> future,
                                GatewayCloudClusterRequest request,
                                GatewayClusterResponse response,
                                Throwable e) {
        ServerWebExchange exchange = request.getExchange();
        WebExchangeUtils.removeAttribute(exchange, Request.KEY_RESPONSE_BODY);
        Supplier<Mono<Void>> supplier = WebExchangeUtils.removeAttribute(exchange, Request.KEY_RESPONSE_WRITE);
        if (e != null) {
            future.completeExceptionally(e);
        } else if (supplier != null) {
            // write data
            supplier.get()
                    .doOnSuccess(v -> future.complete(response))
                    .doOnError(throwable -> future.completeExceptionally(throwable))
                    .subscribe();
        } else {
            future.complete(response);
        }
    }
}
