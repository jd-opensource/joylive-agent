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
@Extension(value = "LaneFilter", order = RouteFilter.ORDER_LANE)
@ConditionalOnProperty(value = GovernanceConfig.CONFIG_LANE_ENABLED, matchIfMissing = true)
public class LaneFilter implements RouteFilter {

    @Override
    public <T extends OutboundRequest> void filter(OutboundInvocation<T> invocation, RouteFilterChain chain) {
        // Retrieve the current route target
        RouteTarget target = invocation.getRouteTarget();
        LaneMetadata metadata = invocation.getLaneMetadata();
        String targetSpaceId = metadata.getTargetSpaceId();
        String targetLaneId = metadata.getTargetLaneId();
        LaneSpace targetSpace = metadata.getTargetSpace();
        String defaultSpaceId = metadata.getDefaultSpaceId();
        String defaultLaneId = metadata.getDefaultLaneId();

        // Check if a target lane is specified
        if (targetSpace != null) {
            // redirect according to the service policy
            ServicePolicy servicePolicy = invocation.getServiceMetadata().getServicePolicy();
            LanePolicy lanePolicy = servicePolicy == null ? null : servicePolicy.getLanePolicy(targetSpaceId);
            String redirect = lanePolicy == null ? null : lanePolicy.getTarget(targetLaneId);
            Lane targetLane = redirect == null || redirect.isEmpty()
                    ? metadata.getTargetLaneOrDefault(targetSpace.getDefaultLane())
                    : targetSpace.getOrDefault(redirect);

            if (targetLane != null) {
                Lane defaultLane = targetSpace.getDefaultLane();
                boolean redirectDefaultLane = defaultLane != null && targetLane != defaultLane;

                // Filter the route target based on the lane space ID and route lane code
                int count = target.filter(e -> e.isLane(targetSpaceId, targetLane.getCode(), defaultSpaceId, defaultLaneId), -1, !redirectDefaultLane);
                // If no matches and a default lane exists, use the default lane
                if (count <= 0 && redirectDefaultLane) {
                    target.filter(e -> e.isLane(targetSpaceId, defaultLane.getCode(), defaultSpaceId, defaultLaneId), -1, true);
                }
            } else {
                String code = redirect == null || redirect.isEmpty() ? targetLaneId : redirect;
                target.filter(e -> e.isLane(targetSpaceId, code, defaultSpaceId, defaultLaneId), -1, true);
            }
        } else {
            // target space is not exists. or empty target space id and without default lane space.
            target.filter(e -> e.isLane(targetSpaceId, targetLaneId, defaultSpaceId, defaultLaneId), -1, true);
        }
        // Proceed with the next filter in the chain
        chain.filter(invocation);
    }
}
