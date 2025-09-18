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
package com.jd.live.agent.plugin.router.springcloud.v1.cluster;

import com.jd.live.agent.core.util.Futures;
import com.jd.live.agent.governance.exception.ErrorPredicate;
import com.jd.live.agent.governance.exception.ServiceError;
import com.jd.live.agent.governance.policy.service.circuitbreak.DegradeConfig;
import com.jd.live.agent.governance.registry.Registry;
import com.jd.live.agent.governance.registry.ServiceEndpoint;
import com.jd.live.agent.plugin.router.springcloud.v1.cluster.context.BlockingClusterContext;
import com.jd.live.agent.plugin.router.springcloud.v1.exception.status.StatusThrowerFactory;
import com.jd.live.agent.plugin.router.springcloud.v1.request.BlockingCloudClusterRequest;
import com.jd.live.agent.plugin.router.springcloud.v1.response.BlockingClusterResponse;
import com.jd.live.agent.plugin.router.springcloud.v1.response.DegradeHttpResponse;
import org.springframework.core.NestedRuntimeException;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * The {@code BlockingCluster} class extends {@code AbstractClientCluster} to provide a blocking
 * mechanism for handling HTTP requests, integrating load balancing and retry logic. It utilizes
 * Spring Cloud's load balancing capabilities to distribute requests across service instances and
 * supports configurable retry mechanisms for handling transient failures.
 *
 * @see AbstractCloudCluster
 */
public class BlockingCloudCluster extends AbstractCloudCluster<
        BlockingCloudClusterRequest,
        BlockingClusterResponse,
        BlockingClusterContext,
        NestedRuntimeException> {

    public BlockingCloudCluster(Registry registry, ClientHttpRequestInterceptor interceptor) {
        super(BlockingClusterContext.of(registry, interceptor), new StatusThrowerFactory<>());
    }

    @Override
    public CompletionStage<BlockingClusterResponse> invoke(BlockingCloudClusterRequest request, ServiceEndpoint endpoint) {
        try {
            ClientHttpResponse response = request.execute(endpoint);
            return CompletableFuture.completedFuture(new BlockingClusterResponse(response));
        } catch (Throwable e) {
            return Futures.future(e);
        }
    }

    @Override
    protected BlockingClusterResponse createResponse(BlockingCloudClusterRequest httpRequest, DegradeConfig degradeConfig) {
        return new BlockingClusterResponse(new DegradeHttpResponse(degradeConfig, httpRequest));
    }

    @Override
    protected BlockingClusterResponse createResponse(ServiceError error, ErrorPredicate predicate) {
        return new BlockingClusterResponse(error, predicate);
    }

}
