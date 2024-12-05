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
package com.jd.live.agent.governance.invoke.cluster;

import com.jd.live.agent.core.extension.annotation.Extensible;
import com.jd.live.agent.governance.instance.Endpoint;
import com.jd.live.agent.governance.invoke.OutboundInvocation;
import com.jd.live.agent.governance.policy.service.cluster.ClusterPolicy;
import com.jd.live.agent.governance.request.ServiceRequest.OutboundRequest;
import com.jd.live.agent.governance.response.ServiceResponse.OutboundResponse;

import java.util.concurrent.CompletionStage;

/**
 * The ClusterInvoker interface defines the contract for executing service requests
 * against a cluster of endpoints. It is responsible for orchestrating the invocation
 * process, which includes selecting endpoints, routing the request, and handling
 * the execution of the request against the live cluster.
 */
@Extensible("ClusterInvoker")
public interface ClusterInvoker {

    String TYPE_FAILFAST = "failfast";

    String TYPE_FAILOVER = "failover";

    String TYPE_FAILSAFE = "failsafe";

    int ORDER_FAILFAST = 0;

    int ORDER_FAILOVER = ORDER_FAILFAST + 1;

    int ORDER_FAILSAFE = ORDER_FAILOVER + 1;

    /**
     * Executes a service request against a live cluster of endpoints. The method handles
     * the entire invocation process, including selecting endpoints based on the provided
     * routing function, invoking the request on the selected endpoints, and returning the
     * corresponding response.
     *
     * @param cluster       The live cluster on which the request will be executed.
     * @param invocation    The outbound invocation logic that defines how the request should be executed.
     * @param defaultPolicy The default cluster policy
     * @param <R>           The type of the outbound request that extends {@link OutboundRequest}.
     * @param <O>           The type of the outbound response that extends {@link OutboundResponse}.
     * @param <E>           The type of the endpoint that extends {@link Endpoint}.
     * @return An outbound response of type {@code O} that corresponds to the executed request.
     */
    <R extends OutboundRequest,
            O extends OutboundResponse,
            E extends Endpoint> CompletionStage<O> execute(LiveCluster<R, O, E> cluster,
                                                            OutboundInvocation<R> invocation,
                                                            ClusterPolicy defaultPolicy);
}
