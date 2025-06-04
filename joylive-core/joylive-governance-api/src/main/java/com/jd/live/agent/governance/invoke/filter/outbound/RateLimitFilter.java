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
import com.jd.live.agent.core.inject.annotation.Injectable;
import com.jd.live.agent.core.util.Futures;
import com.jd.live.agent.governance.annotation.ConditionalOnFlowControlEnabled;
import com.jd.live.agent.governance.instance.Endpoint;
import com.jd.live.agent.governance.invoke.OutboundInvocation;
import com.jd.live.agent.governance.invoke.filter.AbstractRateLimitFilter;
import com.jd.live.agent.governance.invoke.auth.Permission;
import com.jd.live.agent.governance.invoke.filter.OutboundFilter;
import com.jd.live.agent.governance.invoke.filter.OutboundFilterChain;
import com.jd.live.agent.governance.policy.live.FaultType;
import com.jd.live.agent.governance.request.ServiceRequest.OutboundRequest;
import com.jd.live.agent.governance.response.ServiceResponse.OutboundResponse;

import java.util.concurrent.CompletionStage;

/**
 * RateLimitFilter
 */
@Injectable
@Extension(value = "RateLimitFilter", order = OutboundFilter.ORDER_RATE_LIMITER)
@ConditionalOnFlowControlEnabled
public class RateLimitFilter extends AbstractRateLimitFilter implements OutboundFilter {

    @Override
    public <R extends OutboundRequest,
            O extends OutboundResponse,
            E extends Endpoint> CompletionStage<O> filter(OutboundInvocation<R> invocation, E endpoint, OutboundFilterChain chain) {
        Permission result = acquire(invocation);
        if (result != null && !result.isSuccess()) {
            return Futures.future(FaultType.LIMIT.reject(result.getMessage()));
        }
        return chain.filter(invocation, endpoint);
    }

}
