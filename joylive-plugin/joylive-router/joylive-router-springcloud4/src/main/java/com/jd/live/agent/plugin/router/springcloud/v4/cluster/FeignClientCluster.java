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
package com.jd.live.agent.plugin.router.springcloud.v4.cluster;

import com.jd.live.agent.core.util.Futures;
import com.jd.live.agent.core.util.http.HttpStatus;
import com.jd.live.agent.governance.exception.ErrorPredicate;
import com.jd.live.agent.governance.exception.ErrorPredicate.DefaultErrorPredicate;
import com.jd.live.agent.governance.exception.ServiceError;
import com.jd.live.agent.governance.invoke.OutboundInvocation;
import com.jd.live.agent.governance.invoke.cluster.AbstractLiveCluster;
import com.jd.live.agent.governance.policy.service.circuitbreak.DegradeConfig;
import com.jd.live.agent.governance.registry.ServiceEndpoint;
import com.jd.live.agent.plugin.router.springcloud.v4.exception.SpringOutboundThrower;
import com.jd.live.agent.plugin.router.springcloud.v4.exception.feign.FeignThrowerFactory;
import com.jd.live.agent.plugin.router.springcloud.v4.request.FeignClientClusterRequest;
import com.jd.live.agent.plugin.router.springcloud.v4.response.FeignClusterResponse;
import feign.FeignException;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public class FeignClientCluster extends AbstractLiveCluster<FeignClientClusterRequest, FeignClusterResponse, ServiceEndpoint> {

    private static final Set<String> RETRY_EXCEPTIONS = new HashSet<>(Arrays.asList(
            "java.io.IOException",
            "java.util.concurrent.TimeoutException"
    ));

    private static final ErrorPredicate RETRY_PREDICATE = new DefaultErrorPredicate(null, RETRY_EXCEPTIONS);

    private final SpringOutboundThrower<FeignException, FeignClientClusterRequest> thrower = new SpringOutboundThrower<>(new FeignThrowerFactory<>());

    public static final FeignClientCluster INSTANCE = new FeignClientCluster();

    public FeignClientCluster() {
    }

    @Override
    public ErrorPredicate getRetryPredicate() {
        return RETRY_PREDICATE;
    }

    @Override
    public CompletionStage<FeignClusterResponse> invoke(FeignClientClusterRequest request, ServiceEndpoint endpoint) {
        try {
            feign.Response response = request.execute(endpoint);
            return CompletableFuture.completedFuture(new FeignClusterResponse(response));
        } catch (Throwable e) {
            return Futures.future(e);
        }
    }

    @Override
    protected FeignClusterResponse createResponse(FeignClientClusterRequest request) {
        return createResponse(request, DegradeConfig.builder().responseCode(HttpStatus.OK.value()).responseBody("").build());
    }

    @Override
    public CompletionStage<List<ServiceEndpoint>> route(FeignClientClusterRequest request) {
        return request.getInstances();
    }

    @Override
    protected FeignClusterResponse createResponse(FeignClientClusterRequest request, DegradeConfig degradeConfig) {
        return FeignClusterResponse.create(request.getRequest(), degradeConfig);
    }

    @Override
    protected FeignClusterResponse createResponse(ServiceError error, ErrorPredicate predicate) {
        return new FeignClusterResponse(error, predicate);
    }

    @Override
    public Throwable createException(Throwable throwable, FeignClientClusterRequest request) {
        return thrower.createException(throwable, request);
    }

    @Override
    public Throwable createException(Throwable throwable, FeignClientClusterRequest request, ServiceEndpoint endpoint) {
        return thrower.createException(throwable, request, endpoint);
    }

    @Override
    public Throwable createException(Throwable throwable, OutboundInvocation<FeignClientClusterRequest> invocation) {
        return thrower.createException(throwable, invocation);
    }

}
