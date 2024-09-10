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
import com.jd.live.agent.governance.invoke.cluster.LiveCluster;
import com.jd.live.agent.governance.request.ServiceRequest;
import com.jd.live.agent.governance.response.ServiceResponse;

import java.util.concurrent.CompletableFuture;

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

    <R extends ServiceRequest.OutboundRequest,
            O extends ServiceResponse.OutboundResponse,
            E extends Endpoint,
            T extends Throwable>
    CompletableFuture<O> filter(OutboundInvocation<R> invocation, E endpoint, LiveCluster<R, O, E, T> cluster, OutboundFilterChain chain);

}