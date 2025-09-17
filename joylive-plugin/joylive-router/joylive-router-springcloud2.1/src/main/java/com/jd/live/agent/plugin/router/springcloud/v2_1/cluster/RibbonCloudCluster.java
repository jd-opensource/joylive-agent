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
package com.jd.live.agent.plugin.router.springcloud.v2_1.cluster;

import com.jd.live.agent.core.util.Futures;
import com.jd.live.agent.governance.exception.ErrorPredicate;
import com.jd.live.agent.governance.exception.ServiceError;
import com.jd.live.agent.governance.policy.service.circuitbreak.DegradeConfig;
import com.jd.live.agent.governance.registry.Registry;
import com.jd.live.agent.governance.registry.ServiceEndpoint;
import com.jd.live.agent.plugin.router.springcloud.v2_1.cluster.context.HttpClientClusterContext;
import com.jd.live.agent.plugin.router.springcloud.v2_1.exception.httpclient.HttpClientThrowerFactory;
import com.jd.live.agent.plugin.router.springcloud.v2_1.request.HttpClientClusterRequest;
import com.jd.live.agent.plugin.router.springcloud.v2_1.response.RibbonClusterResponse;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.springframework.cloud.netflix.ribbon.apache.RibbonLoadBalancingHttpClient;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import static com.jd.live.agent.plugin.router.springcloud.v2_1.response.RibbonClusterResponse.create;

/**
 * A cluster implementation for Feign clients that manages a group of servers and provides load balancing and failover capabilities.
 *
 * @see AbstractCloudCluster
 */
public class RibbonCloudCluster extends AbstractCloudCluster<
        HttpClientClusterRequest,
        RibbonClusterResponse,
        HttpClientClusterContext,
        IOException> {

    public RibbonCloudCluster(Registry registry, RibbonLoadBalancingHttpClient client) {
        super(new HttpClientClusterContext(registry, client), new HttpClientThrowerFactory<>());
    }

    @Override
    public CompletionStage<RibbonClusterResponse> invoke(HttpClientClusterRequest request, ServiceEndpoint endpoint) {
        try {
            CloseableHttpResponse response = request.execute(endpoint);
            return CompletableFuture.completedFuture(new RibbonClusterResponse(response));
        } catch (Throwable e) {
            return Futures.future(e);
        }
    }

    @Override
    protected RibbonClusterResponse createResponse(HttpClientClusterRequest request, DegradeConfig degradeConfig) {
        return new RibbonClusterResponse(create(request.getRequest(), degradeConfig));
    }

    @Override
    protected RibbonClusterResponse createResponse(ServiceError error, ErrorPredicate predicate) {
        return new RibbonClusterResponse(error, predicate);
    }
}
