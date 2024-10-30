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
import com.jd.live.agent.governance.instance.Endpoint;
import com.jd.live.agent.governance.invoke.OutboundInvocation;
import com.jd.live.agent.governance.request.ServiceRequest.OutboundRequest;
import com.jd.live.agent.governance.response.ServiceResponse.OutboundResponse;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * Defines an interface for the outbound filter chain that handles outbound requests.
 * <p>
 * This interface allows for the sequential processing of outbound requests through a chain of filters. Each filter can
 * perform its processing and decide to pass the request to the next filter in the chain by invoking the {@code filter}
 * method of the chain.
 * </p>
 * <p>
 * The {@link OutboundFilterChain.Chain} inner class provides a concrete implementation of the {@code OutboundFilterChain}, managing the sequence
 * of filters and invoking them in order.
 * </p>
 *
 * @since 1.3.0
 */
public interface OutboundFilterChain {

    /**
     * Filters the outbound service request before it is sent to the remote service.
     *
     * @param invocation The outbound service request invocation.
     * @param endpoint   The endpoint through which the request will be sent.
     * @param <R>        The type of the outbound service request.
     * @param <O>        The type of the outbound service response.
     * @param <E>        The type of the endpoint.
     * @return A CompletableFuture that will contain the filtered outbound service response when the request is completed.
     */
    <R extends OutboundRequest,
            O extends OutboundResponse,
            E extends Endpoint> CompletionStage<O> filter(OutboundInvocation<R> invocation, E endpoint);

    /**
     * A concrete implementation of the {@code OutboundFilterChain} that manages and invokes a sequence of outbound filters.
     */
    class Chain implements OutboundFilterChain {

        /**
         * Tracks the current position in the filter chain
         */
        private int index;

        /**
         * Array of filters in the chain
         */
        private final OutboundFilter[] filters;

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

        @Override
        public <R extends OutboundRequest,
                O extends OutboundResponse,
                E extends Endpoint> CompletionStage<O> filter(OutboundInvocation<R> invocation, E endpoint) {
            CompletionStage<O> result = null;
            if (index < filters.length) {
                result = filters[index++].filter(invocation, endpoint, this);
            } else if (index == filters.length) {
                result = invoke(invocation);
            }
            result = result == null ? CompletableFuture.completedFuture(null) : result;
            return result;
        }

        /**
         * Invokes the inbound request asynchronously.
         *
         * @param invocation The inbound invocation to invoke.
         * @return A completion stage that represents the result of the invocation.
         */
        protected <R extends OutboundRequest, O extends OutboundResponse> CompletionStage<O> invoke(OutboundInvocation<R> invocation) {
            return CompletableFuture.completedFuture(null);
        }

    }

    /**
     * A chain of filters that invokes a callable object.
     */
    class InvokerChain extends Chain {

        private final Callable<Object> invoker;

        /**
         * Creates a new instance of the InvokerChain with the specified filters and invoker.
         *
         * @param filters The list of filters to apply.
         * @param invoker The callable object to invoke.
         */
        public InvokerChain(List<? extends OutboundFilter> filters, Callable<Object> invoker) {
            super(filters);
            this.invoker = invoker;
        }

        /**
         * Creates a new instance of the InvokerChain with the specified filters and invoker.
         *
         * @param filters The array of filters to apply.
         * @param invoker The callable object to invoke.
         */
        public InvokerChain(OutboundFilter[] filters, Callable<Object> invoker) {
            super(filters);
            this.invoker = invoker;
        }

        @SuppressWarnings("unchecked")
        @Override
        protected <R extends OutboundRequest, O extends OutboundResponse> CompletionStage<O> invoke(OutboundInvocation<R> invocation) {
            try {
                if (invoker == null) {
                    return CompletableFuture.completedFuture(null);
                }
                Object result = invoker.call();
                if (result instanceof CompletionStage) {
                    return (CompletionStage<O>) result;
                }
                return CompletableFuture.completedFuture((O) result);
            } catch (Throwable e) {
                return Futures.future(e);
            }
        }
    }
}