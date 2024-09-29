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
import com.jd.live.agent.governance.invoke.InboundInvocation;
import com.jd.live.agent.governance.request.ServiceRequest.InboundRequest;

/**
 * Defines an interface for inbound filters that handle inbound requests.
 * <p>
 * This interface specifies how inbound requests should be handled by defining a {@code filter} method. Implementations
 * of this interface can perform operations on the inbound requests and decide whether to pass the request to the next
 * filter in the chain or to terminate the processing.
 * </p>
 * <p>
 * Filters can be executed in a specified order, which is indicated by the constants {@code ORDER_INBOUND_*}. This allows
 * for a structured and predictable processing of inbound requests.
 * </p>
 *
 * @author Zhiguo.Chen
 * @since 1.0.0
 */
@Extensible(value = "InboundFilter")
public interface InboundFilter {

    /**
     * Execution order for the ready filter
     */
    int ORDER_INBOUND_READY = 0;

    /**
     * Execution order for the request auth filter
     */
    int ORDER_INBOUND_AUTH = ORDER_INBOUND_READY + 1;

    /**
     * Execution order for the request permission filter
     */
    int ORDER_INBOUND_PERMISSION = ORDER_INBOUND_AUTH + 1;

    /**
     * Execution order for the request limiter filter
     */
    int ORDER_INBOUND_LIMITER = ORDER_INBOUND_PERMISSION + 1;

    /**
     * Execution order for the live unit filter
     */
    int ORDER_INBOUND_LIVE_UNIT = ORDER_INBOUND_READY + 1;

    /**
     * Execution order for the live cell filter
     */
    int ORDER_INBOUND_LIVE_CELL = ORDER_INBOUND_LIVE_UNIT + 1;

    /**
     * Execution order for the live failover filter
     */
    int ORDER_INBOUND_LIVE_FAILOVER = ORDER_INBOUND_LIVE_CELL + 1;

    /**
     * Filters an inbound request.
     * <p>
     * Implementations should define this method to perform custom processing on inbound requests. Upon completion of processing,
     * the implementation can choose to pass the control to the next filter in the chain or to stop the execution of the chain.
     * </p>
     *
     * @param invocation Represents the invocation information of an inbound request.
     * @param chain      Represents the filter chain, providing a way to pass control to the next filter in the chain.
     * @param <T>        The type of the inbound request.
     */
    <T extends InboundRequest> void filter(InboundInvocation<T> invocation, InboundFilterChain chain);

}

