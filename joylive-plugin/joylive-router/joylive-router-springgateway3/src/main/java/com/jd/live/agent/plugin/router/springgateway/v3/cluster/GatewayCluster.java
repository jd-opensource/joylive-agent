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
import lombok.Getter;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.reactive.ReactiveLoadBalancer;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.web.server.ServerWebExchange;

import java.util.Set;
import java.util.concurrent.CompletionStage;

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
            ErrorResponseDecorator decorator = new ErrorResponseDecorator(request.getExchange(), policies);
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

}
