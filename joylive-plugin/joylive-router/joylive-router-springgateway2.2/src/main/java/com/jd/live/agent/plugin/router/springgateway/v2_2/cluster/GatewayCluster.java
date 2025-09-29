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
import com.jd.live.agent.plugin.router.springgateway.v2_2.response.ErrorResponseDecorator;
import com.jd.live.agent.plugin.router.springgateway.v2_2.response.GatewayClusterResponse;
import com.jd.live.agent.plugin.router.springgateway.v2_2.util.WebExchangeUtils;
import lombok.Getter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static com.jd.live.agent.plugin.router.springgateway.v2_2.util.WebExchangeUtils.forward;

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
            Set<ErrorPolicy> policies = request.getErrorPolicies();
            // decorate request to transmission
            Propagation propagation = context.getPropagation();
            Carrier carrier = RequestContext.get();
            Consumer<ServerHttpRequest.Builder> header = b -> b.headers(headers ->
                    propagation.write(carrier, new HttpHeadersParser(headers)));
            // decorate response to remove exception header and get body
            boolean failover = request.getAttributeOrDefault(Request.KEY_FAILOVER_REQUEST, Boolean.FALSE);
            ErrorResponseDecorator decorator = new ErrorResponseDecorator(request.getExchange(), policies, failover);
            ServerWebExchange exchange = request.getExchange().mutate().request(header).response(decorator).build();
            GatewayClusterResponse response = new GatewayClusterResponse(exchange);
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
    public void onStartRequest(GatewayCloudClusterRequest request, ServiceEndpoint endpoint) {
        if (endpoint != null) {
            forward(request.getExchange(), endpoint);
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

    @Override
    public void onRetry(GatewayCloudClusterRequest request, int retries) {
        if (retries > 0) {
            ServerWebExchange exchange = request.getExchange();
            WebExchangeUtils.removeAttributes(exchange, Request.KEY_RESPONSE_BODY, Request.KEY_RESPONSE_WRITE, Request.KEY_SERVER_ERROR);
            WebExchangeUtils.closeConnection(exchange);
            WebExchangeUtils.reset(exchange);
        }
    }

    @Override
    public void onRetryComplete(CompletableFuture<GatewayClusterResponse> future,
                                GatewayCloudClusterRequest request,
                                GatewayClusterResponse response,
                                Throwable e) {
        ServerWebExchange exchange = request.getExchange();
        WebExchangeUtils.removeAttributes(exchange, Request.KEY_RESPONSE_BODY, Request.KEY_SERVER_ERROR);
        Supplier<Mono<Void>> supplier = WebExchangeUtils.removeAttribute(exchange, Request.KEY_RESPONSE_WRITE);
        if (e != null) {
            future.completeExceptionally(e);
        } else if (supplier != null) {
            // write data
            supplier.get()
                    .doOnSuccess(v -> future.complete(response))
                    .doOnCancel(() -> future.cancel(true))
                    .doOnError(throwable -> {
                        WebExchangeUtils.closeConnection(exchange);
                        future.completeExceptionally(throwable);
                    })
                    .subscribe();
        } else {
            future.complete(response);
        }
    }

}
