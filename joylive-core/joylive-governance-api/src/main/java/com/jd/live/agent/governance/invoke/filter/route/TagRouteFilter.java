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
import com.jd.live.agent.core.inject.annotation.Injectable;
import com.jd.live.agent.governance.config.GovernanceConfig;
import com.jd.live.agent.governance.invoke.OutboundInvocation;
import com.jd.live.agent.governance.invoke.RouteTarget;
import com.jd.live.agent.governance.invoke.filter.RouteFilter;
import com.jd.live.agent.governance.invoke.filter.RouteFilterChain;
import com.jd.live.agent.governance.invoke.loadbalance.randomweight.RandomWeight;
import com.jd.live.agent.governance.policy.service.ServicePolicy;
import com.jd.live.agent.governance.policy.service.route.RoutePolicy;
import com.jd.live.agent.governance.request.ServiceRequest.OutboundRequest;
import com.jd.live.agent.governance.rule.tag.TagDestination;
import com.jd.live.agent.governance.rule.tag.TagRule;

import java.util.List;

/**
 * TagRouteFilter is a filter that routes requests based on tag rules and policies.
 * It checks the service policy associated with the request and applies tag rules to
 * filter the route targets accordingly. This can be used to implement routing logic
 * based on various tags and their weighted destinations.
 */
@Injectable
@Extension(value = "TagRouteFilter", order = RouteFilter.ORDER_TAG_ROUTE)
@ConditionalOnProperty(value = GovernanceConfig.CONFIG_FLOW_CONTROL_ENABLED, matchIfMissing = true)
public class TagRouteFilter implements RouteFilter {

    @Override
    public <T extends OutboundRequest> void filter(OutboundInvocation<T> invocation, RouteFilterChain chain) {
        RouteTarget target = invocation.getRouteTarget();
        if (!target.isEmpty()) {
            ServicePolicy servicePolicy = invocation.getServiceMetadata().getServicePolicy();
            List<RoutePolicy> policies = servicePolicy == null ? null : servicePolicy.getRoutePolicies();
            if (null != policies && !policies.isEmpty()) {
                for (RoutePolicy policy : policies) {
                    if (match(invocation, policy)) {
                        break;
                    }
                }
            }
        }
        chain.filter(invocation);
    }

    /**
     * Checks if the invocation matches any of the tag rules in the given policy.
     * If a match is found, a destination is selected based on the weighted random
     * selection and the route target is filtered accordingly.
     *
     * @param invocation The outbound invocation context.
     * @param policy The route policy containing tag rules and destinations.
     * @param <T> The type parameter of the outbound request.
     * @return true if a match is found and the filter is applied, false otherwise.
     */
    private <T extends OutboundRequest> boolean match(OutboundInvocation<T> invocation, RoutePolicy policy) {
        for (TagRule rule : policy.getTagRules()) {
            if (rule.match(invocation)) {
                TagDestination destination = RandomWeight.select(rule.getDestinations(), TagDestination::getWeight);
                if (destination != null) {
                    invocation.getRouteTarget().filter(destination::match);
                }
                return true;
            }
        }
        return false;
    }
}
