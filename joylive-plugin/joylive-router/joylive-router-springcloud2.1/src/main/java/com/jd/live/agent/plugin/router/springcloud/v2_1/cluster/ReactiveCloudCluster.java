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
import com.jd.live.agent.plugin.router.springcloud.v2_1.cluster.context.ReactiveClusterContext;
import com.jd.live.agent.plugin.router.springcloud.v2_1.exception.reactive.WebClientThrowerFactory;
import com.jd.live.agent.plugin.router.springcloud.v2_1.instance.InstanceEndpoint;
import com.jd.live.agent.plugin.router.springcloud.v2_1.request.ReactiveCloudClusterRequest;
import com.jd.live.agent.plugin.router.springcloud.v2_1.response.ReactiveClusterResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClientException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.concurrent.CompletionStage;

import static com.jd.live.agent.plugin.router.springcloud.v2_1.response.ReactiveClusterResponse.create;

/**
 * Represents a client cluster that handles outbound requests and responses for services
 * within a microservices architecture, utilizing a reactive load balancer. This class
 * integrates with Spring's WebClient and load balancing infrastructure to dynamically
 * select service instances based on load balancing strategies and policies.
 */
public class ReactiveCloudCluster extends AbstractCloudCluster<
        ReactiveCloudClusterRequest,
        ReactiveClusterResponse,
        ReactiveClusterContext,
        WebClientException> {

    public ReactiveCloudCluster(ExchangeFilterFunction filterFunction) {
        super(new ReactiveClusterContext(filterFunction), new WebClientThrowerFactory<>());
    }

    @Override
    public CompletionStage<ReactiveClusterResponse> invoke(ReactiveCloudClusterRequest request, InstanceEndpoint endpoint) {
        try {
            return request.exchange(endpoint).map(ReactiveClusterResponse::new).toFuture();
        } catch (Throwable e) {
            return Futures.future(e);
        }
    }

    @Override
    protected ReactiveClusterResponse createResponse(ReactiveCloudClusterRequest request, DegradeConfig degradeConfig) {
        return create(request, degradeConfig);
    }

    @Override
    protected ReactiveClusterResponse createResponse(ServiceError error, ErrorPredicate predicate) {
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        Throwable throwable = error.getThrowable();
        if (throwable instanceof WebClientResponseException) {
            status = ((WebClientResponseException) throwable).getStatusCode();
        }
        String errorMessage = error.getError();
        errorMessage = errorMessage == null && throwable != null ? throwable.getMessage() : errorMessage;
        errorMessage = errorMessage == null ? status.getReasonPhrase() : errorMessage;
        return new ReactiveClusterResponse(ClientResponse.create(status).body(errorMessage).build());
    }

}
