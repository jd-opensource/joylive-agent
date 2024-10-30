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
import com.jd.live.agent.core.util.Futures;
import com.jd.live.agent.governance.instance.Endpoint;
import com.jd.live.agent.governance.invoke.OutboundInvocation;
import com.jd.live.agent.governance.invoke.counter.Counter;
import com.jd.live.agent.governance.invoke.filter.OutboundFilter;
import com.jd.live.agent.governance.invoke.filter.OutboundFilterChain;
import com.jd.live.agent.governance.policy.live.FaultType;
import com.jd.live.agent.governance.request.ServiceRequest.OutboundRequest;
import com.jd.live.agent.governance.response.ServiceResponse.OutboundResponse;

import java.util.concurrent.CompletionStage;

/**
 * A filter that counts the number of active requests and their execution time for each endpoint.
 * It also provides a mechanism to limit the maximum number of active requests per endpoint.
 *
 * @see OutboundFilter
 */
@Extension(value = "CounterFilter", order = OutboundFilter.ORDER_COUNTER)
public class CounterFilter implements OutboundFilter {

    @Override
    public <R extends OutboundRequest,
            O extends OutboundResponse,
            E extends Endpoint> CompletionStage<O> filter(OutboundInvocation<R> invocation, E endpoint, OutboundFilterChain chain) {
        Counter counter = endpoint == null ? null : endpoint.getAttribute(Endpoint.ATTRIBUTE_COUNTER);
        if (counter != null) {
            counter.getService().tryClean(invocation.getInstances());
            if (!counter.begin(0)) {
                return Futures.future(FaultType.LIMIT.reject("Has reached the maximum number of active requests."));
            }
            long startTime = System.currentTimeMillis();
            CompletionStage<O> stage = chain.filter(invocation, endpoint);
            return stage.whenComplete((o, r) -> {
                long elapsed = System.currentTimeMillis() - startTime;
                if (r == null) {
                    counter.success(elapsed);
                } else {
                    counter.fail(elapsed);
                }
            });
        } else {
            return chain.filter(invocation, endpoint);
        }

    }

}
