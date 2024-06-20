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
package com.jd.live.agent.governance.interceptor;

import com.jd.live.agent.core.instance.Location;
import com.jd.live.agent.core.plugin.definition.InterceptorAdaptor;
import com.jd.live.agent.governance.invoke.InvocationContext;
import com.jd.live.agent.governance.policy.GovernancePolicy;
import com.jd.live.agent.governance.policy.live.*;
import com.jd.live.agent.governance.policy.variable.UnitFunction;

/**
 * AbstractMQConsumerInterceptor
 */
public abstract class AbstractMQConsumerInterceptor extends InterceptorAdaptor {

    protected final InvocationContext context;

    protected final boolean liveEnabled;

    protected final boolean laneEnabled;

    public AbstractMQConsumerInterceptor(InvocationContext context, boolean liveEnabled, boolean laneEnabled) {
        this.context = context;
        this.liveEnabled = liveEnabled;
        this.laneEnabled = laneEnabled;
    }

    /**
     * Determines whether an operation is allowed based on the provided liveSpaceId, ruleId, and variable.
     *
     * @param liveSpaceId the ID of the live space to check against. Can be null or empty.
     * @param ruleId the ID of the rule to check against. Can be null or empty.
     * @param variable the variable to check against the rule. Can be null or empty.
     * @return {@code true} if the operation is allowed; {@code false} otherwise.
     */
    protected boolean allowLive(String liveSpaceId, String ruleId, String variable) {
        Location location = context.getApplication().getLocation();
        String currentLiveSpaceId = location.getLiveSpaceId();
        if (liveSpaceId == null || liveSpaceId.isEmpty()) {
            return currentLiveSpaceId == null || currentLiveSpaceId.isEmpty();
        } else if (!liveSpaceId.equals(currentLiveSpaceId)) {
            return false;
        }
        GovernancePolicy policy = context.getPolicySupplier().getPolicy();
        LiveSpace liveSpace = policy == null ? null : policy.getLiveSpace(liveSpaceId);
        UnitRule rule = liveSpace == null ? null : liveSpace.getUnitRule(ruleId);
        if (rule == null) {
            return true;
        }
        if (variable == null || variable.isEmpty()) {
            Unit center = liveSpace.getCenter();
            return rule.getVariableMissingAction() == VariableMissingAction.CENTER
                    && center != null
                    && center.getCode().equals(location.getUnit());
        }
        UnitFunction func = context.getUnitFunction(rule.getVariableFunction());
        UnitRoute route = rule.getUnitRoute(variable, func);
        Unit routeUnit = route == null ? null : route.getUnit();
        return routeUnit != null
                && routeUnit.getCode().equals(location.getUnit())
                && routeUnit.getAccessMode().isWriteable();
    }

    /**
     * Determines whether the current lane and lane space match the provided laneSpaceId and laneId.
     *
     * @param laneSpaceId the ID of the lane space to check against. Can be null or empty.
     * @param laneId the ID of the lane to check against. Can be null or empty.
     * @return {@code true} if the current lane space and lane match the provided IDs; {@code false} otherwise.
     */
    protected boolean allowLane(String laneSpaceId, String laneId) {
        Location location = context.getApplication().getLocation();
        String currentLaneSpaceId = location.getLaneSpaceId();
        String currentLane = location.getLane();
        if (laneSpaceId == null || laneSpaceId.isEmpty()) {
            return currentLaneSpaceId == null || currentLaneSpaceId.isEmpty();
        } else if (!laneSpaceId.equals(currentLaneSpaceId)) {
            return false;
        } else if (laneId == null || laneId.isEmpty()) {
            return currentLane == null || currentLane.isEmpty();
        } else {
            return laneId.equals(currentLane);
        }
    }

}
