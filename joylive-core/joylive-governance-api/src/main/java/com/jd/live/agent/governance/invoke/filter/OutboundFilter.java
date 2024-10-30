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
import com.jd.live.agent.governance.instance.Endpoint;
import com.jd.live.agent.governance.invoke.OutboundInvocation;
import com.jd.live.agent.governance.request.ServiceRequest.OutboundRequest;
import com.jd.live.agent.governance.response.ServiceResponse.OutboundResponse;

import java.util.concurrent.CompletionStage;

/**
 * Defines an interface for outbound filters that handle outbound requests.
 * <p>
 * This interface specifies how outbound requests should be handled by defining a {@code filter} method. Implementations
 * of this interface can perform operations on the outbound requests and decide whether to pass the request to the next
 * filter in the chain or to terminate the processing.
 * </p>
 * <p>
 * Filters can be executed in a specified order, which is indicated by the constants {@code ORDER_OUTBOUND_*}. This allows
 * for a structured and predictable processing of outbound requests.
 * </p>
 *
 * @since 1.3.0
 */
@Extensible(value = "OutboundFilter")
public interface OutboundFilter {

    int ORDER_COUNTER = 100;

    int ORDER_FAULT_INJECTION = ORDER_COUNTER + 100;

    int ORDER_AUTH = ORDER_FAULT_INJECTION + 100;

    /**
     * Filters the outbound service request before it is sent to the remote service.
     *
     * @param invocation The outbound service request invocation.
     * @param endpoint   The endpoint through which the request will be sent.
     * @param chain      The filter chain that this filter is part of.
     * @param <R>        The type of the outbound service request.
     * @param <O>        The type of the outbound service response.
     * @param <E>        The type of the endpoint.
     * @return A CompletionStage that will contain the filtered outbound service response when the request is completed.
     */
    <R extends OutboundRequest,
            O extends OutboundResponse,
            E extends Endpoint>
    CompletionStage<O> filter(OutboundInvocation<R> invocation, E endpoint, OutboundFilterChain chain);

}