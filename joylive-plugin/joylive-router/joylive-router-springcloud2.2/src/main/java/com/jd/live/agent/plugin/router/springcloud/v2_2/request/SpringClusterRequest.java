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
package com.jd.live.agent.plugin.router.springcloud.v2_2.request;

import com.jd.live.agent.governance.policy.service.cluster.RetryPolicy;
import com.jd.live.agent.governance.request.HttpRequest.HttpOutboundRequest;
import org.springframework.cloud.client.ServiceInstance;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * An interface that extends {@link HttpOutboundRequest} to provide additional methods
 * for managing Spring Cloud cluster-related operations, such as retrieving service instances,
 * handling lifecycle events, and defining retry policies.
 */
public interface SpringClusterRequest extends HttpOutboundRequest {

    /**
     * Retrieves a list of available service instances from the cluster.
     *
     * @return a {@link Mono} emitting a list of {@link ServiceInstance} objects
     */
    Mono<List<ServiceInstance>> getInstances();

    /**
     * Retrieves the default retry policy for the request.
     *
     * @return the default {@link RetryPolicy} instance
     */
    RetryPolicy getDefaultRetryPolicy();
}

