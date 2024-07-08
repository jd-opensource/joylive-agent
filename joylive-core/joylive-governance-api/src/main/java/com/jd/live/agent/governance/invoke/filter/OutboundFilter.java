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
package com.jd.live.agent.governance.invoke.filter;

import com.jd.live.agent.core.extension.annotation.Extensible;
import com.jd.live.agent.governance.invoke.OutboundInvocation;
import com.jd.live.agent.governance.request.ServiceRequest.OutboundRequest;

/**
 * Defines the contract for route filters which are responsible for filtering target instances
 * during the outbound request routing process. Implementations of this interface can be used to
 * apply various criteria to select or modify the target instances based on attributes like
 * liveliness, locality, retries, stickiness, health, tags, lanes, cell information, and load
 * balancing strategies.
 * <p>
 * Implementations can be ordered using predefined constants to determine the sequence in which
 * filters are applied.
 *
 * @author Zhiguo.Chen
 * @since 1.0.0
 */
@Extensible(value = "OutboundFilter")
public interface OutboundFilter {

    int ORDER_CIRCUIT_BREAKER = 100;

    int ORDER_STICKY = ORDER_CIRCUIT_BREAKER + 100;

    int ORDER_LOCALHOST = ORDER_STICKY + 100;

    int ORDER_HEALTH = ORDER_LOCALHOST + 100;

    int ORDER_VIRTUAL = ORDER_HEALTH + 100;

    int ORDER_LIVE_UNIT = ORDER_VIRTUAL + 100;

    int ORDER_TAG_ROUTE = ORDER_LIVE_UNIT + 100;

    int ORDER_LANE = ORDER_TAG_ROUTE + 100;

    int ORDER_LIVE_CELL = ORDER_LANE + 100;

    int ORDER_RETRY = ORDER_LIVE_CELL + 100;

    int ORDER_LOADBALANCE = ORDER_LIVE_CELL + 100;

    int ORDER_INSTANCE_CIRCUIT_BREAKER = ORDER_LOADBALANCE + 100;

    /**
     * Applies the filter logic to the given outbound invocation. This method is called as part of a
     * chain of filters, and it is responsible for invoking the next filter in the chain or terminating
     * the chain if the criteria for routing are not met.
     *
     * @param <T>        The type of the outbound request.
     * @param invocation The outbound invocation containing the request and additional routing information.
     * @param chain      The chain of route filters that should be applied to the invocation.
     */
    <T extends OutboundRequest> void filter(OutboundInvocation<T> invocation, OutboundFilterChain chain);

    /**
     * Represents a filter for live routes.
     */
    interface LiveRouteFilter extends OutboundFilter {

    }

}
