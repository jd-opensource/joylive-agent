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
package com.jd.live.agent.governance.invoke.filter.outbound;

import com.jd.live.agent.core.extension.annotation.Extension;
import com.jd.live.agent.governance.instance.Endpoint;
import com.jd.live.agent.governance.invoke.OutboundInvocation;
import com.jd.live.agent.governance.invoke.cluster.LiveCluster;
import com.jd.live.agent.governance.invoke.filter.OutboundFilter;
import com.jd.live.agent.governance.invoke.filter.OutboundFilterChain;
import com.jd.live.agent.governance.request.ServiceRequest.OutboundRequest;
import com.jd.live.agent.governance.response.ServiceResponse.OutboundResponse;

import java.util.concurrent.CompletionStage;

/**
 * An extension of the OutboundFilter interface that handles the actual invocation of the outbound service request.
 * This filter is responsible for invoking the request on the live cluster.
 *
 * @see OutboundFilter
 */
@Extension(value = "InvokeFilter", order = OutboundFilter.ORDER_INVOKE)
public class InvokeFilter implements OutboundFilter {

    @Override
    public <R extends OutboundRequest,
            O extends OutboundResponse,
            E extends Endpoint,
            T extends Throwable> CompletionStage<O> filter(LiveCluster<R, O, E, T> cluster,
                                                           OutboundInvocation<R> invocation,
                                                           E endpoint,
                                                           OutboundFilterChain chain) {
        return cluster.invoke(invocation.getRequest(), endpoint);
    }
}
