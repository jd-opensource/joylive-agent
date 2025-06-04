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
package com.jd.live.agent.governance.invoke.filter.inbound;

import com.jd.live.agent.core.extension.annotation.Extension;
import com.jd.live.agent.core.inject.annotation.Injectable;
import com.jd.live.agent.core.util.Futures;
import com.jd.live.agent.governance.annotation.ConditionalOnFlowControlEnabled;
import com.jd.live.agent.governance.invoke.InboundInvocation;
import com.jd.live.agent.governance.invoke.filter.AbstractRateLimitFilter;
import com.jd.live.agent.governance.invoke.auth.Permission;
import com.jd.live.agent.governance.invoke.filter.InboundFilter;
import com.jd.live.agent.governance.invoke.filter.InboundFilterChain;
import com.jd.live.agent.governance.policy.live.FaultType;
import com.jd.live.agent.governance.request.ServiceRequest.InboundRequest;

import java.util.concurrent.CompletionStage;

/**
 * RateLimitFilter
 */
@Injectable
@Extension(value = "RateLimitFilter", order = InboundFilter.ORDER_RATE_LIMITER)
@ConditionalOnFlowControlEnabled
public class RateLimitFilter extends AbstractRateLimitFilter implements InboundFilter {

    @Override
    public <T extends InboundRequest> CompletionStage<Object> filter(InboundInvocation<T> invocation, InboundFilterChain chain) {
        Permission result = acquire(invocation);
        if (result != null && !result.isSuccess()) {
            return Futures.future(FaultType.LIMIT.reject(result.getMessage()));
        }
        return chain.filter(invocation);
    }

}
