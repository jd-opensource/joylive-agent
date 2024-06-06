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

    int ORDER_RANDOM_WEIGHT = 0;

    int ORDER_ROUND_ROBIN = ORDER_RANDOM_WEIGHT + 1;

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

    /**
     * A delegate class for load balancing that forwards all its operations to another {@link LoadBalancer} instance.
     * This class acts as a wrapper or intermediary, allowing for additional behaviors to be inserted before or after
     * the delegation of the load balancing task. It implements the {@link LoadBalancer} interface and can be used
     * anywhere a LoadBalancer is required, providing a flexible mechanism for extending or modifying load balancing
     * behavior dynamically.
     *
     * @see LoadBalancer
     */
    class LoadBalancerDelegate implements LoadBalancer {

        /**
         * The {@link LoadBalancer} instance to which this delegate will forward all method calls.
         */
        protected LoadBalancer delegate;

        /**
         * Constructs a new {@code LoadBalancerDelegate} with a specified {@link LoadBalancer} to delegate to.
         *
         * @param delegate The {@link LoadBalancer} instance that this delegate will forward calls to.
         */
        public LoadBalancerDelegate(LoadBalancer delegate) {
            this.delegate = delegate;
        }

        /**
         * Delegates the selection of an endpoint to the underlying {@link LoadBalancer} instance. This method
         * is called to select an appropriate {@link Endpoint} from a list of available endpoints based on the
         * current load balancing strategy.
         *
         * @param <T>        The type of {@link Endpoint} to be selected.
         * @param endpoints  A list of available endpoints from which one will be selected.
         * @param invocation The invocation context, which may contain metadata or other information used in the
         *                   selection process.
         * @return The selected {@link Endpoint}, or {@code null} if no suitable endpoint could be found.
         */
        @Override
        public <T extends Endpoint> T doSelect(List<T> endpoints, Invocation<?> invocation) {
            return delegate.doSelect(endpoints, invocation);
        }
    }

}
