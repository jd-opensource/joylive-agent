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

import com.jd.live.agent.bootstrap.logger.Logger;
import com.jd.live.agent.bootstrap.logger.LoggerFactory;
import com.jd.live.agent.governance.invoke.OutboundInvocation;
import com.jd.live.agent.governance.request.ServiceRequest.OutboundRequest;

import java.util.Collection;

/**
 * Defines an interface for a routing filter chain that filters target instances.
 * <p>
 * This interface facilitates the sequential processing of outbound requests through a series of routing filters. Each filter
 * can perform its own processing and decide whether to pass the request to the next filter in the chain.
 * </p>
 * <p>
 * The {@link Chain} inner class provides a concrete implementation of the {@code OutboundFilterChain}, managing the sequence
 * of filters and invoking them in order.
 * </p>
 *
 * @author Zhiguo.Chen
 * @since 1.0.0
 */
public interface RouteFilterChain {

    /**
     * Processes the outbound request through the chain of filters.
     *
     * @param invocation Represents the invocation information of an outbound request.
     * @param <T>        The type of the outbound request.
     */
    <T extends OutboundRequest> void filter(OutboundInvocation<T> invocation);

    /**
     * A concrete implementation of the {@code OutboundFilterChain} that manages and invokes a sequence of routing filters.
     */
    class Chain implements RouteFilterChain {

        private static final Logger logger = LoggerFactory.getLogger(Chain.class);

        private int index; // Tracks the current position in the filter chain.
        private final RouteFilter[] filters; // Array of filters in the chain.
        private final int size;

        /**
         * Constructs a chain with an array of routing filters.
         *
         * @param filters An array of routing filters. If null, the chain will be empty.
         */
        @SafeVarargs
        public <K extends RouteFilter> Chain(final K... filters) {
            this.filters = filters == null ? new RouteFilter[0] : filters;
            this.size = this.filters.length;
        }

        /**
         * Constructs a chain with a collection of routing filters.
         *
         * @param filters A collection of routing filters. If null, the chain will be empty.
         */
        public Chain(final Collection<? extends RouteFilter> filters) {
            this.filters = filters == null ? new RouteFilter[0] : filters.toArray(new RouteFilter[0]);
            this.size = this.filters.length;
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
            if (index < size) {
                if (logger.isDebugEnabled()) {
                    log(index - 1, invocation);
                }
                RouteFilter filter = filters[index++];
                filter.filter(invocation, this);
            } else if (index == size && logger.isDebugEnabled()) {
                log(index - 1, invocation);
            }
        }

        private <T extends OutboundRequest> void log(int index, OutboundInvocation<T> invocation) {
            if (index < 0) {
                logger.debug("Before apply any filter, endpoint size: " + invocation.getEndpointSize());
            } else {
                logger.debug("After apply " + filters[index].getClass().getSimpleName() + ", endpoint size: " + invocation.getEndpointSize());
            }
        }
    }
}

