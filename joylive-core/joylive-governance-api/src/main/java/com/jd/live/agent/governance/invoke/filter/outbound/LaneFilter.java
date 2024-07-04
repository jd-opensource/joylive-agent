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

import com.jd.live.agent.core.extension.annotation.ConditionalOnProperty;
import com.jd.live.agent.core.extension.annotation.Extension;
import com.jd.live.agent.core.inject.annotation.Injectable;
import com.jd.live.agent.core.instance.GatewayRole;
import com.jd.live.agent.governance.config.GovernanceConfig;
import com.jd.live.agent.governance.invoke.OutboundInvocation;
import com.jd.live.agent.governance.invoke.RouteTarget;
import com.jd.live.agent.governance.invoke.filter.OutboundFilter;
import com.jd.live.agent.governance.invoke.filter.OutboundFilterChain;
import com.jd.live.agent.governance.invoke.metadata.LaneMetadata;
import com.jd.live.agent.governance.policy.lane.Lane;
import com.jd.live.agent.governance.policy.lane.LaneSpace;
import com.jd.live.agent.governance.policy.service.ServicePolicy;
import com.jd.live.agent.governance.policy.service.lane.LanePolicy;
import com.jd.live.agent.governance.request.ServiceRequest.OutboundRequest;

/**
 * Filters route targets based on lane metadata. This filter ensures that only instances
 * belonging to the correct lane are considered for routing requests. If no instances are
 * found in the target lane, it may fall back to the default lane if one is specified.
 *
 * @since 1.0.0
 */
@Injectable
@Extension(value = "LaneFilter", order = OutboundFilter.ORDER_LANE)
@ConditionalOnProperty(value = GovernanceConfig.CONFIG_LANE_ENABLED, matchIfMissing = true)
public class LaneFilter implements OutboundFilter {

    @Override
    public <T extends OutboundRequest> void filter(OutboundInvocation<T> invocation, OutboundFilterChain chain) {
        LaneMetadata laneMetadata = invocation.getLaneMetadata();
        LaneSpace laneSpace = laneMetadata.getLaneSpace();
        Lane targetLane = laneMetadata.getTargetLane();

        // Check if a target lane is specified
        if (targetLane != null) {
            // Get the application and gateway role from the invocation context
            GatewayRole gatewayRole = invocation.getContext().getApplication().getService().getGateway();
            // Retrieve the service policy  from the service metadata
            ServicePolicy servicePolicy = invocation.getServiceMetadata().getServicePolicy();
            Boolean autoJoin = gatewayRole == GatewayRole.FRONTEND
                    ? Boolean.TRUE
                    : (servicePolicy == null ? null : servicePolicy.getAutoLaneEnabled());
            autoJoin = autoJoin == null ? laneMetadata.getLaneConfig().isAutoJoinEnabled() : autoJoin;

            LanePolicy lanePolicy = servicePolicy == null ? null : servicePolicy.getLanePolicy(laneSpace.getId());
            String redirect = lanePolicy == null ? null : lanePolicy.getTarget(targetLane.getCode());
            Lane routeLane = redirect == null || redirect.isEmpty()
                    ? (autoJoin ? targetLane : null)
                    : laneSpace.getOrDefault(redirect);

            if (routeLane != null) {
                // Retrieve the current route target
                RouteTarget target = invocation.getRouteTarget();

                // Check if there is a default lane and if the target lane is different
                Lane defaultLane = laneSpace.getDefaultLane();
                boolean withDefaultLane = defaultLane != null && targetLane != defaultLane;

                // Filter the route target based on the lane space ID and route lane code
                int count = target.filter(e -> e.isLane(laneSpace.getId(), routeLane.getCode()), -1, !withDefaultLane);

                // If no matches and a default lane exists, use the default lane
                if (count <= 0 && withDefaultLane) {
                    target.filter(e -> e.isLane(laneSpace.getId(), defaultLane.getCode()), -1, true);
                }
            }
        }

        // Proceed with the next filter in the chain
        chain.filter(invocation);
    }
}
