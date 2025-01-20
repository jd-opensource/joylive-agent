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
package com.jd.live.agent.governance.invoke.filter.route;

import com.jd.live.agent.core.extension.annotation.Extension;
import com.jd.live.agent.governance.annotation.ConditionalOnFlowControlEnabled;
import com.jd.live.agent.governance.invoke.OutboundInvocation;
import com.jd.live.agent.governance.invoke.RouteTarget;
import com.jd.live.agent.governance.invoke.filter.RouteFilter;
import com.jd.live.agent.governance.invoke.filter.RouteFilterChain;
import com.jd.live.agent.governance.request.ServiceRequest.OutboundRequest;

import java.util.Set;

/**
 * RetryFilter is a filter that excludes endpoints that have previously failed
 * during the current request's attempt history. This filter ensures that failed
 * endpoints are not retried, which can help in avoiding repeated failures and
 * potentially improve the system's reliability.
 *
 * @since 1.0.0
 */
@Extension(value = "RetryFilter", order = RouteFilter.ORDER_RETRY)
@ConditionalOnFlowControlEnabled
public class RetryFilter implements RouteFilter {

    @Override
    public <T extends OutboundRequest> void filter(final OutboundInvocation<T> invocation, final RouteFilterChain chain) {
        RouteTarget target = invocation.getRouteTarget();
        // Get the set of attempted endpoint IDs from the request
        Set<String> attempts = invocation.getRequest().getAttempts();
        // If there have been previous attempts, filter out the endpoints that have already failed
        if (attempts != null && !attempts.isEmpty()) {
            // Can retry on failed instances
            target.filter(endpoint -> !attempts.contains(endpoint.getId()), -1, false);
        }
        chain.filter(invocation);
    }
}
