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
import com.jd.live.agent.governance.invoke.OutboundInvocation;
import com.jd.live.agent.governance.request.ServiceRequest.OutboundRequest;

/**
 * Defines an interface for outbound filters that handle outbound requests.
 * <p>
 * This interface specifies how outbound requests should be handled by defining a {@code filter} method. Implementations
 * of this interface can perform operations on the outbound requests and decide whether to pass the request to the next
 * filter in the chain or to terminate the processing.
 * </p>
 * <p>
 * Filters can be executed in a specified order, which is indicated by the constant {@code ORDER_OUTBOUND_LIVE_UNIT}. This allows
 * for a structured and predictable processing of outbound requests.
 * </p>
 *
 * @author Zhiguo.Chen
 * @since 1.0.0
 */
@Extensible(value = "OutboundFilter")
public interface OutboundFilter {

    // Constant defining the order of execution for the filters
    int ORDER_OUTBOUND_LIVE_UNIT = 0; // Execution order for the live unit filter

    /**
     * Filters an outbound request.
     * <p>
     * Implementations should define this method to perform custom processing on outbound requests. Upon completion of processing,
     * the implementation can choose to pass the control to the next filter in the chain or to stop the execution of the chain.
     * </p>
     *
     * @param invocation Represents the invocation information of an outbound request.
     * @param chain      Represents the filter chain, providing a way to pass control to the next filter in the chain.
     * @param <T>        The type of the outbound request.
     */
    <T extends OutboundRequest> void filter(OutboundInvocation<T> invocation, OutboundFilterChain chain);

}
