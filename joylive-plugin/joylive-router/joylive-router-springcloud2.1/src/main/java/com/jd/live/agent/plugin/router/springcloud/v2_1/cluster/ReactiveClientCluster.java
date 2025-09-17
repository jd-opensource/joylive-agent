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
import com.jd.live.agent.governance.exception.ErrorPredicate.DefaultErrorPredicate;
import com.jd.live.agent.governance.exception.ServiceError;
import com.jd.live.agent.governance.invoke.OutboundInvocation;
import com.jd.live.agent.governance.invoke.cluster.AbstractLiveCluster;
import com.jd.live.agent.governance.policy.service.circuitbreak.DegradeConfig;
import com.jd.live.agent.governance.registry.ServiceEndpoint;
import com.jd.live.agent.plugin.router.springcloud.v2_1.exception.SpringOutboundThrower;
import com.jd.live.agent.plugin.router.springcloud.v2_1.exception.reactive.WebClientThrowerFactory;
import com.jd.live.agent.plugin.router.springcloud.v2_1.request.ReactiveClientClusterRequest;
import com.jd.live.agent.plugin.router.springcloud.v2_1.response.ReactiveClusterResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClientException;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletionStage;

import static com.jd.live.agent.plugin.router.springcloud.v2_1.response.ReactiveClusterResponse.create;

public class ReactiveClientCluster extends AbstractLiveCluster<ReactiveClientClusterRequest, ReactiveClusterResponse, ServiceEndpoint> {

    private static final Set<String> RETRY_EXCEPTIONS = new HashSet<>(Arrays.asList(
            "java.io.IOException",
            "java.util.concurrent.TimeoutException"
    ));

    private static final ErrorPredicate RETRY_PREDICATE = new DefaultErrorPredicate(null, RETRY_EXCEPTIONS);

    private final SpringOutboundThrower<WebClientException, ReactiveClientClusterRequest> thrower = new SpringOutboundThrower<>(new WebClientThrowerFactory<>());

    public static final ReactiveClientCluster INSTANCE = new ReactiveClientCluster();

    public ReactiveClientCluster() {
    }

    @Override
    public ErrorPredicate getRetryPredicate() {
        return RETRY_PREDICATE;
    }

    @Override
    public CompletionStage<ReactiveClusterResponse> invoke(ReactiveClientClusterRequest request, ServiceEndpoint endpoint) {
        try {
            return request.exchange(endpoint).toFuture().thenApply(ReactiveClusterResponse::new);
        } catch (Throwable e) {
            return Futures.future(e);
        }
    }

    @Override
    protected ReactiveClusterResponse createResponse(ReactiveClientClusterRequest request) {
        return createResponse(request, DegradeConfig.builder().responseCode(HttpStatus.OK.value()).responseBody("").build());
    }

    @Override
    public CompletionStage<List<ServiceEndpoint>> route(ReactiveClientClusterRequest request) {
        return request.getInstances();
    }

    @Override
    protected ReactiveClusterResponse createResponse(ReactiveClientClusterRequest request, DegradeConfig degradeConfig) {
        return create(request.getRequest(), degradeConfig, ExchangeStrategies.withDefaults());
    }

    @Override
    protected ReactiveClusterResponse createResponse(ServiceError error, ErrorPredicate predicate) {
        return new ReactiveClusterResponse(error, predicate);
    }

    @Override
    public Throwable createException(Throwable throwable, ReactiveClientClusterRequest request) {
        return thrower.createException(throwable, request);
    }

    @Override
    public Throwable createException(Throwable throwable, ReactiveClientClusterRequest request, ServiceEndpoint endpoint) {
        return thrower.createException(throwable, request, endpoint);
    }

    @Override
    public Throwable createException(Throwable throwable, OutboundInvocation<ReactiveClientClusterRequest> invocation) {
        return thrower.createException(throwable, invocation);
    }

}
