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
package com.jd.live.agent.governance.invoke.loadbalance;

import com.jd.live.agent.core.extension.annotation.Extensible;
import com.jd.live.agent.governance.instance.Endpoint;
import com.jd.live.agent.governance.invoke.Invocation;
import com.jd.live.agent.governance.policy.service.loadbalance.LoadBalancePolicy;

import java.util.List;

/**
 * LoadBalancer interface for selecting an endpoint based on a specific strategy.
 * This interface defines methods to choose and elect endpoints from a list.
 *
 * @since 1.0.0
 */
@Extensible("LoadBalancer")
public interface LoadBalancer {

    /**
     * Order value for random weight strategy.
     */
    int ORDER_RANDOM_WEIGHT = 0;

    /**
     * Order value for round-robin strategy.
     */
    int ORDER_ROUND_ROBIN = ORDER_RANDOM_WEIGHT + 1;

    /**
     * Order value for shortest-response strategy.
     */
    int ORDER_SHORTEST_RESPONSE = ORDER_ROUND_ROBIN + 1;

    /**
     * Order value for weight-response strategy.
     */
    int ORDER_WEIGHT_RESPONSE = ORDER_SHORTEST_RESPONSE + 1;

    /**
     * Elects a candidate endpoint from the list based on the invocation context and the specified load balancing policy.
     * The method selects an endpoint according to the rules defined by the provided policy and the current invocation.
     * If the list of endpoints is empty or null, this method returns {@code null}.
     *
     * @param <T>        The type of the endpoint, which must extend {@link Endpoint}.
     * @param endpoints  The list of endpoints to elect from.
     * @param policy     The load balancing policy used to determine the selection strategy.
     * @param invocation The invocation context, which provides additional information for the selection process.
     * @return The elected candidate endpoint, or {@code null} if the list is empty or null.
     */
    <T extends Endpoint> Candidate<T> elect(List<T> endpoints, LoadBalancePolicy policy, Invocation<?> invocation);

}

