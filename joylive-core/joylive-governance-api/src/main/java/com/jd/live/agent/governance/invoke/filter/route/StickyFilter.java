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
import com.jd.live.agent.governance.context.bag.Carrier;
import com.jd.live.agent.governance.instance.Endpoint;
import com.jd.live.agent.governance.invoke.OutboundInvocation;
import com.jd.live.agent.governance.invoke.OutboundListener;
import com.jd.live.agent.governance.invoke.RouteTarget;
import com.jd.live.agent.governance.invoke.filter.RouteFilter;
import com.jd.live.agent.governance.invoke.filter.RouteFilterChain;
import com.jd.live.agent.governance.policy.service.ServicePolicy;
import com.jd.live.agent.governance.policy.service.loadbalance.StickyType;
import com.jd.live.agent.governance.request.Request;
import com.jd.live.agent.governance.request.ServiceRequest.OutboundRequest;
import com.jd.live.agent.governance.request.StickySession;
import com.jd.live.agent.governance.request.StickySessionFactory;

/**
 * StickyFilter is a filter that prioritizes routing to the same instance that was previously
 * used for a successful request. This is known as "stickiness" and can be useful for maintaining
 * session state or ensuring consistency in processing across multiple requests.
 *
 * @since 1.0.0
 */
@Extension(value = "StickyFilter", order = RouteFilter.ORDER_STICKY)
@ConditionalOnFlowControlEnabled
public class StickyFilter implements RouteFilter {

    @Override
    public <T extends OutboundRequest> void filter(OutboundInvocation<T> invocation, RouteFilterChain chain) {
        T request = invocation.getRequest();
        ServicePolicy servicePolicy = invocation.getServiceMetadata().getServicePolicy();
        StickySessionFactory factory = servicePolicy == null ? null : servicePolicy.getLoadBalancePolicy();
        StickySession session = request.getStickySession(factory);
        StickyType stickyType = session == null ? StickyType.NONE : session.getStickyType();
        Carrier carrier = request.getCarrier();
        if (stickyType != null && stickyType != StickyType.NONE) {
            RouteTarget target = invocation.getRouteTarget();
            // Get the sticky ID from the request, if available
            String id = session.getStickyId();
            // first remove sticky id from context
            String ctxId = carrier == null ? null : carrier.removeAttribute(Request.KEY_STICKY_ID);
            final String stickyId = id != null && !id.isEmpty() ? id : ctxId;
            // If a sticky ID is available, filter the targets to only include the one with the sticky ID
            if (stickyId != null && !stickyId.isEmpty()) {
                if (stickyType == StickyType.FIXED) {
                    target.filter(endpoint -> stickyId.equals(endpoint.getId()), 1);
                } else {
                    carrier = carrier != null ? carrier : request.getOrCreateCarrier();
                    carrier.setAttribute(Request.KEY_STICKY_ID, stickyId);
                }
            }
            invocation.addListener(new StickyListener(session));
        } else if (carrier != null) {
            carrier.removeAttribute(Request.KEY_STICKY_ID);
        }
        chain.filter(invocation);
    }

    /**
     * A listener that sets a sticky ID on the request of an outbound invocation.
     */
    private static class StickyListener implements OutboundListener {

        private final StickySession session;

        StickyListener(StickySession session) {
            this.session = session;
        }

        @Override
        public void onForward(Endpoint endpoint, OutboundInvocation<?> invocation) {
            if (endpoint != null) {
                session.setStickyId(endpoint.getId());
            }
        }

    }
}
