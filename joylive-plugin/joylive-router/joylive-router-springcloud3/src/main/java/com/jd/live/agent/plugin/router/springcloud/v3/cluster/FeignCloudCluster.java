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
package com.jd.live.agent.plugin.router.springcloud.v3.cluster;

import com.jd.live.agent.core.util.Futures;
import com.jd.live.agent.governance.exception.ErrorPredicate;
import com.jd.live.agent.governance.exception.ErrorPredicate.DefaultErrorPredicate;
import com.jd.live.agent.governance.exception.ServiceError;
import com.jd.live.agent.governance.policy.service.circuitbreak.DegradeConfig;
import com.jd.live.agent.governance.registry.Registry;
import com.jd.live.agent.governance.registry.ServiceEndpoint;
import com.jd.live.agent.plugin.router.springcloud.v3.cluster.context.FeignClusterContext;
import com.jd.live.agent.plugin.router.springcloud.v3.request.FeignCloudClusterRequest;
import com.jd.live.agent.plugin.router.springcloud.v3.response.FeignClusterResponse;
import feign.Client;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import static com.jd.live.agent.plugin.router.springcloud.v3.response.FeignClusterResponse.create;

/**
 * A cluster implementation for Feign clients that manages a group of servers and provides load balancing and failover capabilities.
 *
 * @see AbstractCloudCluster
 */
public class FeignCloudCluster extends AbstractCloudCluster<FeignCloudClusterRequest, FeignClusterResponse, FeignClusterContext> {

    private static final Set<String> RETRY_EXCEPTIONS = new HashSet<>(Arrays.asList(
            "java.io.IOException",
            "java.util.concurrent.TimeoutException",
            "org.springframework.cloud.client.loadbalancer.reactive.RetryableStatusCodeException"
    ));

    private static final ErrorPredicate RETRY_PREDICATE = new DefaultErrorPredicate(null, RETRY_EXCEPTIONS);

    public FeignCloudCluster(Registry registry, Client client) {
        super(new FeignClusterContext(registry, client));
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
    public ErrorPredicate getRetryPredicate() {
        return RETRY_PREDICATE;
    }

    @Override
    public void onSuccess(FeignClusterResponse response, FeignCloudClusterRequest request, ServiceEndpoint endpoint) {
        request.onSuccess(response, endpoint);
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
