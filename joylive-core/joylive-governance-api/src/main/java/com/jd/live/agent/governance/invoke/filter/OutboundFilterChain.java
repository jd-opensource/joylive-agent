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

import com.jd.live.agent.governance.invoke.OutboundInvocation;
import com.jd.live.agent.governance.request.ServiceRequest.OutboundRequest;

import java.util.List;

/**
 * Defines an interface for the outbound filter chain that handles outbound requests.
 * <p>
 * This interface allows for the sequential processing of outbound requests through a chain of filters. Each filter can
 * perform its processing and decide to pass the request to the next filter in the chain by invoking the {@code filter}
 * method of the chain.
 * </p>
 * <p>
 * The {@link Chain} inner class provides a concrete implementation of the {@code OutboundFilterChain}, managing the sequence
 * of filters and invoking them in order.
 * </p>
 *
 * @author Zhiguo.Chen
 * @since 1.0.0
 */
public interface OutboundFilterChain {

    /**
     * Processes the outbound request through the chain of filters.
     *
     * @param invocation Represents the invocation information of an outbound request.
     * @param <T>        The type of the outbound request.
     */
    <T extends OutboundRequest> void filter(OutboundInvocation<T> invocation);

    /**
     * A concrete implementation of the {@code OutboundFilterChain} that manages and invokes a sequence of outbound filters.
     */
    class Chain implements OutboundFilterChain {

        private int index; // Tracks the current position in the filter chain
        private final OutboundFilter[] filters; // Array of filters in the chain

        /**
         * Constructs a chain with a list of outbound filters.
         *
         * @param filters A list of outbound filters. Can be null, in which case the chain will be empty.
         */
        public Chain(List<? extends OutboundFilter> filters) {
            this.filters = filters == null ? new OutboundFilter[0] : filters.toArray(new OutboundFilter[0]);
        }

        /**
         * Constructs a chain with an array of outbound filters.
         *
         * @param filters An array of outbound filters. Can be null, in which case the chain will be empty.
         */
        public Chain(OutboundFilter... filters) {
            this.filters = filters == null ? new OutboundFilter[0] : filters;
        }

        /**
         * Processes the outbound request through the chain of filters.
         * <p>
         * This method sequentially invokes the {@code filter} method of each filter in the chain until the chain is
         * exhausted or a filter decides to terminate the processing.
         * </p>
         *
         * @param invocation Represents the invocation information of an outbound request.
         * @param <T>        The type of the outbound request.
         */
        @Override
        public <T extends OutboundRequest> void filter(OutboundInvocation<T> invocation) {
            if (index < filters.length) {
                filters[index++].filter(invocation, this);
            }
        }
    }
}

