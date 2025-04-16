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
import com.jd.live.agent.plugin.router.springcloud.v2_1.cluster.context.FeignClusterContext;
import com.jd.live.agent.plugin.router.springcloud.v2_1.exception.status.StatusThrowerFactory;
import com.jd.live.agent.plugin.router.springcloud.v2_1.request.FeignCloudClusterRequest;
import com.jd.live.agent.plugin.router.springcloud.v2_1.response.FeignClusterResponse;
import feign.Client;
import org.springframework.core.NestedRuntimeException;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import static com.jd.live.agent.plugin.router.springcloud.v2_1.response.FeignClusterResponse.create;

/**
 * A cluster implementation for Feign clients that manages a group of servers and provides load balancing and failover capabilities.
 *
 * @see AbstractCloudCluster
 */
public class FeignCloudCluster extends AbstractCloudCluster<
        FeignCloudClusterRequest,
        FeignClusterResponse,
        FeignClusterContext,
        NestedRuntimeException> {

    public FeignCloudCluster(Registry registry, Client client) {
        super(new FeignClusterContext(registry, client), new StatusThrowerFactory<>());
    }

    @Override
    public CompletionStage<FeignClusterResponse> invoke(FeignCloudClusterRequest request, ServiceEndpoint endpoint) {
        try {
            feign.Response response = request.execute(endpoint);
            return CompletableFuture.completedFuture(new FeignClusterResponse(response));
        } catch (Throwable e) {
            return Futures.future(e);
        }
    }

    @Override
    protected FeignClusterResponse createResponse(FeignCloudClusterRequest request, DegradeConfig degradeConfig) {
        return create(request.getRequest(), degradeConfig);
    }

    @Override
    protected FeignClusterResponse createResponse(ServiceError error, ErrorPredicate predicate) {
        return new FeignClusterResponse(error, predicate);
    }
}
