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

import com.jd.live.agent.core.extension.annotation.ConditionalOnProperty;
import com.jd.live.agent.core.extension.annotation.Extension;
import com.jd.live.agent.governance.config.GovernanceConfig;
import com.jd.live.agent.governance.context.RequestContext;
import com.jd.live.agent.governance.invoke.OutboundInvocation;
import com.jd.live.agent.governance.invoke.RouteTarget;
import com.jd.live.agent.governance.invoke.filter.RouteFilter;
import com.jd.live.agent.governance.invoke.filter.RouteFilterChain;
import com.jd.live.agent.governance.policy.service.loadbalance.StickyType;
import com.jd.live.agent.governance.request.Request;
import com.jd.live.agent.governance.request.ServiceRequest.OutboundRequest;

/**
 * StickyFilter is a filter that prioritizes routing to the same instance that was previously
 * used for a successful request. This is known as "stickiness" and can be useful for maintaining
 * session state or ensuring consistency in processing across multiple requests.
 */
@Extension(value = "StickyFilter", order = RouteFilter.ORDER_STICKY)
@ConditionalOnProperty(value = GovernanceConfig.CONFIG_FLOW_CONTROL_ENABLED, matchIfMissing = true)
public class StickyFilter implements RouteFilter {

    @Override
    public <T extends OutboundRequest> void filter(OutboundInvocation<T> invocation, RouteFilterChain chain) {
        StickyType stickyType = invocation.getServiceMetadata().getStickyType();
        if (stickyType != StickyType.NONE) {
            RouteTarget target = invocation.getRouteTarget();
            // Get the sticky ID from the request, if available
            String id = invocation.getRequest().getStickyId();
            // first remove sticky id from context
            String ctxId = RequestContext.removeAttribute(Request.KEY_STICKY_ID);
            final String stickyId = id != null && !id.isEmpty() ? id : ctxId;
            // If a sticky ID is available, filter the targets to only include the one with the sticky ID
            if (stickyId != null && !stickyId.isEmpty()) {
                if (stickyType == StickyType.FIXED) {
                    target.filter(endpoint -> stickyId.equals(endpoint.getId()), 1);
                } else {
                    RequestContext.setAttribute(Request.KEY_STICKY_ID, stickyId);
                }
            }
        } else {
            RequestContext.removeAttribute(Request.KEY_STICKY_ID);
        }
        chain.filter(invocation);
    }
}
