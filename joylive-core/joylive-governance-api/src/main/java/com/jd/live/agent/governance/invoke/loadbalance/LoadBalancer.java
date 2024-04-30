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

import java.util.List;

/**
 * LoadBalancer interface for selecting an endpoint based on a specific strategy.
 *
 * @since 1.0.0
 */
@Extensible("LoadBalancer")
public interface LoadBalancer {

    /**
     * Chooses an endpoint from a list based on the invocation context. If the list is null or empty,
     * returns null. If the list contains only one endpoint, returns that endpoint. Otherwise,
     * {@link #doSelect(List, Invocation)} is called to select an endpoint.
     *
     * @param <T>        The type of the endpoints, extending from Endpoint.
     * @param endpoints  The list of endpoints to choose from.
     * @param invocation The invocation context.
     * @return The chosen endpoint, or null if no endpoints are available.
     */
    default <T extends Endpoint> T choose(List<T> endpoints, Invocation<?> invocation) {
        if (null == endpoints || endpoints.isEmpty()) {
            return null;
        } else if (endpoints.size() == 1) {
            return endpoints.get(0);
        }
        return doSelect(endpoints, invocation);
    }

    /**
     * Selects an endpoint from a list based on the invocation context using a specific load balancing strategy.
     * This method is intended to be implemented by subclasses to provide custom selection logic.
     *
     * @param <T>        The type of the endpoints, extending from Endpoint.
     * @param endpoints  The list of endpoints from which to select.
     * @param invocation The invocation context.
     * @return The selected endpoint.
     */
    <T extends Endpoint> T doSelect(List<T> endpoints, Invocation<?> invocation);

}
