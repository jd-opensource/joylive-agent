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

import com.jd.live.agent.governance.invoke.InboundInvocation;
import com.jd.live.agent.governance.request.ServiceRequest.InboundRequest;

import java.util.List;

/**
 * Defines an interface for the inbound filter chain that handles inbound requests.
 * <p>
 * This interface allows for the sequential processing of inbound requests through a chain of filters. Each filter can
 * perform its processing and decide to pass the request to the next filter in the chain by invoking the {@code filter}
 * method of the chain.
 * </p>
 * <p>
 * The {@link Chain} inner class provides a concrete implementation of the {@code InboundFilterChain}, managing the sequence
 * of filters and invoking them in order.
 * </p>
 *
 * @author Zhiguo.Chen
 * @since 1.0.0
 */
public interface InboundFilterChain {

    /**
     * Processes the inbound request through the chain of filters.
     *
     * @param invocation Represents the invocation information of an inbound request.
     * @param <T>        The type of the inbound request.
     */
    <T extends InboundRequest> void filter(InboundInvocation<T> invocation);

    /**
     * A concrete implementation of the {@code InboundFilterChain} that manages and invokes a sequence of inbound filters.
     */
    class Chain implements InboundFilterChain {

        private int index; // Tracks the current position in the filter chain
        private final InboundFilter[] filters; // Array of filters in the chain

        /**
         * Constructs a chain with a list of inbound filters.
         *
         * @param filters A list of inbound filters. Can be null, in which case the chain will be empty.
         */
        public Chain(List<? extends InboundFilter> filters) {
            this.filters = filters == null ? new InboundFilter[0] : filters.toArray(new InboundFilter[0]);
        }

        /**
         * Constructs a chain with an array of inbound filters.
         *
         * @param filters An array of inbound filters. Can be null, in which case the chain will be empty.
         */
        public Chain(InboundFilter... filters) {
            this.filters = filters == null ? new InboundFilter[0] : filters;
        }

        /**
         * Processes the inbound request through the chain of filters.
         * <p>
         * This method sequentially invokes the {@code filter} method of each filter in the chain until the chain is
         * exhausted or a filter decides to terminate the processing.
         * </p>
         *
         * @param invocation Represents the invocation information of an inbound request.
         * @param <T>        The type of the inbound request.
         */
        @Override
        public <T extends InboundRequest> void filter(InboundInvocation<T> invocation) {
            if (index < filters.length) {
                filters[index++].filter(invocation, this);
            }
        }

    }
}

