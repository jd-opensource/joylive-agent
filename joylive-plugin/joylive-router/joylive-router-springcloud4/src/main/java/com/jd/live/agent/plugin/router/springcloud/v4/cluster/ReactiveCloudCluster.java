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
import com.jd.live.agent.governance.exception.ErrorPredicate;
import com.jd.live.agent.governance.exception.ServiceError;
import com.jd.live.agent.governance.policy.service.circuitbreak.DegradeConfig;
import com.jd.live.agent.plugin.router.springcloud.v4.cluster.context.ReactiveClusterContext;
import com.jd.live.agent.plugin.router.springcloud.v4.exception.reactive.WebClientThrowerFactory;
import com.jd.live.agent.plugin.router.springcloud.v4.instance.SpringEndpoint;
import com.jd.live.agent.plugin.router.springcloud.v4.request.ReactiveCloudClusterRequest;
import com.jd.live.agent.plugin.router.springcloud.v4.response.ReactiveClusterResponse;
import org.springframework.cloud.client.loadbalancer.reactive.LoadBalancedExchangeFilterFunction;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletionStage;

import static com.jd.live.agent.plugin.router.springcloud.v4.response.ReactiveClusterResponse.create;

/**
 * Represents a client cluster that handles outbound requests and responses for services
 * within a microservices architecture, utilizing a reactive load balancer. This class
 * integrates with Spring's WebClient and load balancing infrastructure to dynamically
 * select service instances based on load balancing strategies and policies.
 */
public class ReactiveCloudCluster extends AbstractCloudCluster<
        ReactiveCloudClusterRequest,
        ReactiveClusterResponse,
        ReactiveClusterContext> {

    private static final Set<String> RETRY_EXCEPTIONS = new HashSet<>(Arrays.asList(
            "java.io.IOException",
            "java.util.concurrent.TimeoutException",
            "org.springframework.cloud.client.loadbalancer.reactive.RetryableStatusCodeException"
    ));

    private static final ErrorPredicate RETRY_PREDICATE = new ErrorPredicate.DefaultErrorPredicate(null, RETRY_EXCEPTIONS);

    public ReactiveCloudCluster(ReactiveClusterContext context) {
        super(context, new WebClientThrowerFactory<>());
    }

    public ReactiveCloudCluster(LoadBalancedExchangeFilterFunction filterFunction) {
        super(new ReactiveClusterContext(filterFunction), new WebClientThrowerFactory<>());
    }

    @Override
    public CompletionStage<ReactiveClusterResponse> invoke(ReactiveCloudClusterRequest request, SpringEndpoint endpoint) {
        try {
            return request.exchange(endpoint.getInstance()).map(ReactiveClusterResponse::new).toFuture();
        } catch (Throwable e) {
            return Futures.future(e);
        }
    }

    @Override
    public ErrorPredicate getRetryPredicate() {
        return RETRY_PREDICATE;
    }

    @Override
    public void onSuccess(ReactiveClusterResponse response, ReactiveCloudClusterRequest request, SpringEndpoint endpoint) {
        request.onSuccess(response, endpoint);
    }

    @Override
    protected ReactiveClusterResponse createResponse(ReactiveCloudClusterRequest request, DegradeConfig degradeConfig) {
        return create(request, degradeConfig);
    }

    @Override
    protected ReactiveClusterResponse createResponse(ServiceError error, ErrorPredicate predicate) {
        HttpStatusCode status = HttpStatus.INTERNAL_SERVER_ERROR;
        Throwable throwable = error.getThrowable();
        if (throwable instanceof WebClientResponseException) {
            status = ((WebClientResponseException) throwable).getStatusCode();
        }
        String errorMessage = error.getError();
        errorMessage = errorMessage == null && throwable != null ? throwable.getMessage() : errorMessage;
        errorMessage = errorMessage == null ? ((HttpStatus) status).getReasonPhrase() : errorMessage;
        return new ReactiveClusterResponse(ClientResponse.create(status).body(errorMessage).build());
    }

}
