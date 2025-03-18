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
package com.jd.live.agent.plugin.router.springcloud.v2_2.cluster;

import com.jd.live.agent.core.util.Futures;
import com.jd.live.agent.governance.exception.ErrorPredicate;
import com.jd.live.agent.governance.exception.ServiceError;
import com.jd.live.agent.governance.policy.service.circuitbreak.DegradeConfig;
import com.jd.live.agent.plugin.router.springcloud.v2_2.cluster.context.BlockingClusterContext;
import com.jd.live.agent.plugin.router.springcloud.v2_2.instance.SpringEndpoint;
import com.jd.live.agent.plugin.router.springcloud.v2_2.request.BlockingCloudClusterRequest;
import com.jd.live.agent.plugin.router.springcloud.v2_2.response.BlockingClusterResponse;
import com.jd.live.agent.plugin.router.springcloud.v2_2.response.DegradeHttpResponse;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
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
        BlockingClusterContext> {

    private static final Set<String> RETRY_EXCEPTIONS = new HashSet<>(Arrays.asList(
            "java.io.IOException",
            "java.util.concurrent.TimeoutException",
            "org.springframework.cloud.client.loadbalancer.reactive.RetryableStatusCodeException"
    ));

    private static final ErrorPredicate RETRY_PREDICATE = new ErrorPredicate.DefaultErrorPredicate(null, RETRY_EXCEPTIONS);

    public BlockingCloudCluster(BlockingClusterContext context) {
        super(context);
    }

    public BlockingCloudCluster(ClientHttpRequestInterceptor interceptor) {
        super(new BlockingClusterContext(interceptor));
    }

    @Override
    public CompletionStage<BlockingClusterResponse> invoke(BlockingCloudClusterRequest request, SpringEndpoint endpoint) {
        // TODO sticky session
        try {
            ClientHttpResponse response = request.execute(endpoint.getInstance());
            return CompletableFuture.completedFuture(new BlockingClusterResponse(response));
        } catch (Throwable e) {
            return Futures.future(e);
        }
    }

    @Override
    public ErrorPredicate getRetryPredicate() {
        return RETRY_PREDICATE;
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
