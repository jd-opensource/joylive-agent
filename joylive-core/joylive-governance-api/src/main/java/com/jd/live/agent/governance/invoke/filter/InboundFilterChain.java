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

import com.jd.live.agent.core.util.Futures;
import com.jd.live.agent.governance.invoke.InboundInvocation;
import com.jd.live.agent.governance.request.ServiceRequest.InboundRequest;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * Defines an interface for the inbound filter chain that handles inbound requests.
 * <p>
 * This interface allows for the sequential processing of inbound requests through a chain of filters. Each filter can
 * perform its processing and decide to pass the request to the next filter in the chain by invoking the {@code filter}
 * method of the chain.
 * </p>
 * <p>
 * The {@link DefaultInboundFilterChain} inner class provides a concrete implementation of the {@code InboundFilterChain}, managing the sequence
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
    <T extends InboundRequest> CompletionStage<Object> filter(InboundInvocation<T> invocation);

    /**
     * A concrete implementation of the {@code InboundFilterChain} that manages and invokes a sequence of inbound filters.
     */
    class DefaultInboundFilterChain implements InboundFilterChain {

        /**
         * Tracks the current position in the filter chain
         */
        protected int index;
        /**
         * Array of filters in the chain
         */
        protected final InboundFilter[] filters;

        protected final Callable<Object> callable;

        /**
         * Constructs a chain with an array of inbound filters.
         *
         * @param filters An array of inbound filters. Can be null, in which case the chain will be empty.
         */
        public DefaultInboundFilterChain(InboundFilter... filters) {
            this(filters, null);
        }

        /**
         * Constructs a chain with an array of inbound filters.
         *
         * @param filters  An array of inbound filters. Can be null, in which case the chain will be empty.
         * @param callable A callable that will be invoked when the filter chain is exhausted. Can be null.
         */
        public DefaultInboundFilterChain(InboundFilter[] filters, Callable<Object> callable) {
            this.filters = filters == null ? new InboundFilter[0] : filters;
            this.callable = callable;
        }

        @Override
        public <T extends InboundRequest> CompletionStage<Object> filter(InboundInvocation<T> invocation) {
            CompletionStage<Object> result = null;
            if (index < filters.length) {
                result = filters[index++].filter(invocation, this);
            } else if (index == filters.length) {
                result = callable == null ? CompletableFuture.completedFuture(null) : call(callable);
            }
            return result == null ? CompletableFuture.completedFuture(null) : result;
        }

        /**
         * Invokes the inbound request asynchronously.
         *
         * @param invocation The inbound invocation to invoke.
         * @return A completion stage that represents the result of the invocation.
         */
        protected <T extends InboundRequest> CompletionStage<Object> invoke(InboundInvocation<T> invocation) {
            return CompletableFuture.completedFuture(null);
        }

        protected CompletionStage<Object> call(final Callable<Object> callable) {
            try {
                Object value = callable.call();
                if (value instanceof CompletionStage) {
                    return (CompletionStage<Object>) value;
                } else {
                    return CompletableFuture.completedFuture(value);
                }
            } catch (Throwable e) {
                return Futures.future(e);
            }
        }
    }
}

