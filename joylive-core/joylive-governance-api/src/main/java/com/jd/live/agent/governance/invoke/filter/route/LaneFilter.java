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
import com.jd.live.agent.core.inject.annotation.Injectable;
import com.jd.live.agent.governance.annotation.ConditionalOnLaneEnabled;
import com.jd.live.agent.governance.invoke.OutboundInvocation;
import com.jd.live.agent.governance.invoke.RouteTarget;
import com.jd.live.agent.governance.invoke.filter.RouteFilter;
import com.jd.live.agent.governance.invoke.filter.RouteFilterChain;
import com.jd.live.agent.governance.invoke.metadata.LaneMetadata;
import com.jd.live.agent.governance.policy.lane.FallbackType;
import com.jd.live.agent.governance.policy.lane.Lane;
import com.jd.live.agent.governance.policy.service.lane.LanePolicy;
import com.jd.live.agent.governance.request.ServiceRequest.OutboundRequest;

import java.util.function.Function;

/**
 * Filters route targets based on lane metadata. This filter ensures that only instances
 * belonging to the correct lane are considered for routing requests. If no instances are
 * found in the target lane, it may fall back to the default lane if one is specified.
 *
 * @since 1.0.0
 */
@Injectable
@Extension(value = "LaneFilter", order = RouteFilter.ORDER_LANE)
@ConditionalOnLaneEnabled
public class LaneFilter implements RouteFilter {

    @Override
    public <T extends OutboundRequest> void filter(final OutboundInvocation<T> invocation, final RouteFilterChain chain) {
        // Early return optimization - check null metadata first
        LaneMetadata metadata = invocation.getLaneMetadata();
        if (metadata == null) {
            chain.filter(invocation);
            return;
        }

        // Cache frequently accessed objects to avoid repeated method calls
        RouteTarget target = invocation.getRouteTarget();
        String targetSpaceId = metadata.getTargetSpaceId();
        String targetLaneId = metadata.getTargetLaneId();
        String defaultSpaceId = metadata.getDefaultSpaceId();
        String defaultLaneId = metadata.getDefaultLaneId();

        // Fast path for no target space scenario
        if (metadata.getTargetSpace() == null) {
            target.filter(e -> e.isLane(targetSpaceId, targetLaneId, defaultSpaceId, defaultLaneId), -1, true);
            chain.filter(invocation);
            return;
        }

        // Get target lane and service policy
        Lane targetLane = metadata.getTargetLane();
        LanePolicy lanePolicy = invocation.getServiceMetadata().getLanePolicy(targetSpaceId);
        // Determine fallback strategy efficiently
        FallbackLane fallbackLane = fallback(lanePolicy, targetLane, targetLaneId, defaultLaneId);
        // Handle the case when target lane is null
        if (targetLane == null) {
            target.filter(e -> e.isLane(targetSpaceId, fallbackLane.lane, defaultSpaceId, defaultLaneId), -1, true);
            chain.filter(invocation);
            return;
        }

        // Determine if fallback strategies are needed
        boolean redirect = fallbackLane.redirect(targetLaneId);
        // keep original instances when fallback
        int count = target.filter(e -> e.isLane(targetSpaceId, targetLaneId, defaultSpaceId, defaultLaneId), -1, !redirect);
        // Apply fallback strategies only when main filter returns no results
        if (count <= 0 && redirect) {
            target.filter(e -> e.isLane(targetSpaceId, fallbackLane.lane, defaultSpaceId, defaultLaneId), -1, true);
        }
        chain.filter(invocation);
    }

    /**
     * Determines the fallback strategy by checking service policy first, then lane configuration
     */
    private FallbackLane fallback(final LanePolicy lanePolicy,
                                  final Lane targetLane,
                                  final String targetLaneId,
                                  final String defaultLaneId) {
        FallbackType fallbackType = null;
        String fallbackLane = null;
        // Service-level fallback takes precedence
        if (lanePolicy != null) {
            fallbackType = lanePolicy.getFallbackType();
            fallbackLane = getFallbackLane(fallbackType, defaultLaneId, targetLaneId, lanePolicy::getFallbackLane);
        }
        // Lane-level fallback as secondary option
        if (fallbackType == null && targetLane != null) {
            fallbackType = targetLane.getFallbackType();
            fallbackLane = getFallbackLane(fallbackType, defaultLaneId, targetLaneId, i -> targetLane.getFallbackLane());
        }
        // Default fallback if nothing specified
        if (fallbackType == null) {
            fallbackType = FallbackType.DEFAULT;
            fallbackLane = defaultLaneId;
        }

        return new FallbackLane(fallbackType, fallbackLane);
    }

    /**
     * Determines the fallback lane ID based on the specified fallback type.
     *
     * @param type          the fallback type strategy
     * @param defaultLaneId the default lane ID to use as fallback
     * @param targetLaneId  the target lane ID
     * @param customer      function to get custom lane ID for CUSTOM fallback type
     * @return the fallback lane ID, or null if type is null
     */
    private String getFallbackLane(final FallbackType type,
                                   final String defaultLaneId,
                                   final String targetLaneId,
                                   final Function<String, String> customer) {
        if (type == null) {
            return null;
        }
        switch (type) {
            case CUSTOM:
                String result = customer.apply(targetLaneId);
                return result == null ? defaultLaneId : result;
            case REJECT:
                return targetLaneId;
            default:
                return defaultLaneId;
        }
    }

    /**
     * Configuration holder for fallback strategy
     */
    private static class FallbackLane {
        final FallbackType type;
        final String lane;

        FallbackLane(FallbackType type, String lane) {
            this.type = type;
            this.lane = lane;
        }

        /**
         * Determines if redirect should occur based on fallback type and target lane comparison.
         * Redirect is allowed when fallback type is not REJECT and target lane differs from current lane.
         *
         * @param targetLane the target lane identifier to compare against current lane
         * @return true if redirect should occur, false otherwise
         */
        public boolean redirect(String targetLane) {
            return type != FallbackType.REJECT && !targetLane.equals(lane);
        }
    }
}
