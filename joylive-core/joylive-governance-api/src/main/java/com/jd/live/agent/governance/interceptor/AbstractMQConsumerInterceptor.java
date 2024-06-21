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
import com.jd.live.agent.governance.config.MqConfig;
import com.jd.live.agent.governance.invoke.InvocationContext;
import com.jd.live.agent.governance.policy.GovernancePolicy;
import com.jd.live.agent.governance.policy.live.LiveSpace;
import com.jd.live.agent.governance.policy.live.Unit;
import com.jd.live.agent.governance.policy.live.UnitRoute;
import com.jd.live.agent.governance.policy.live.UnitRule;
import com.jd.live.agent.governance.policy.variable.UnitFunction;

import java.util.HashMap;
import java.util.Map;

/**
 * AbstractMQConsumerInterceptor
 */
public abstract class AbstractMQConsumerInterceptor extends InterceptorAdaptor {

    protected final InvocationContext context;

    public AbstractMQConsumerInterceptor(InvocationContext context) {
        this.context = context;
    }

    /**
     * Determines whether an operation is allowed based on the provided liveSpaceId, ruleId, and variable.
     *
     * @param liveSpaceId the ID of the live space to check against. Can be null or empty.
     * @param ruleId the ID of the rule to check against. Can be null or empty.
     * @param variable the variable to check against the rule. Can be null or empty.
     * @return {@code MessageAction.CONSUME} if the operation is allowed;
     *         {@code MessageAction.DISCARD} if the operation is not allowed;
     *         {@code MessageAction.REJECT} if the operation is explicitly rejected.
     */
    protected MessageAction allowLive(String liveSpaceId, String ruleId, String variable) {
        if (!context.isLiveEnabled()) {
            return MessageAction.CONSUME;
        } else if (liveSpaceId == null || liveSpaceId.isEmpty()) {
            return MessageAction.CONSUME;
        } else if (!liveSpaceId.equals(context.getLocation().getLiveSpaceId())) {
            return MessageAction.DISCARD;
        } else {
            GovernancePolicy policy = context.getPolicySupplier().getPolicy();
            LiveSpace liveSpace = policy == null ? null : policy.getLiveSpace(liveSpaceId);
            Unit local = liveSpace == null ? null : liveSpace.getCurrentUnit();
            UnitRule rule = liveSpace == null ? null : liveSpace.getUnitRule(ruleId);
            if (liveSpace == null) {
                return MessageAction.CONSUME;
            } else if (local == null) {
                return MessageAction.DISCARD;
            } else if (rule == null) {
                return MessageAction.DISCARD;
            } else if (!local.getAccessMode().isWriteable()) {
                // TODO REJECT
                return rule.isFailover(local.getCode()) ? MessageAction.DISCARD : MessageAction.REJECT;
            } else {
                UnitFunction func = context.getUnitFunction(rule.getVariableFunction());
                UnitRoute targetRoute = rule.getUnitRoute(variable, func);
                Unit targetUnit = targetRoute == null ? null : targetRoute.getUnit();
                if (targetUnit == null) {
                    return MessageAction.DISCARD;
                } else if (targetUnit == local) {
                    return MessageAction.CONSUME;
                } else {
                    return local.getCode().equals(targetRoute.getFailoverUnit()) ? MessageAction.CONSUME : MessageAction.DISCARD;
                }
            }
        }
    }

    /**
     * Determines whether the current lane and lane space match the provided laneSpaceId and laneId.
     *
     * @param laneSpaceId the ID of the lane space to check against. Can be null or empty.
     * @param laneId the ID of the lane to check against. Can be null or empty.
     * @return {@code MessageAction.CONSUME} if the current lane space and lane match the provided IDs;
     *         {@code MessageAction.DISCARD} otherwise.
     */
    protected MessageAction allowLane(String laneSpaceId, String laneId) {
        return context.getLocation().inLane(laneSpaceId, laneId)
                ? MessageAction.CONSUME
                : MessageAction.DISCARD;
    }

    /**
     * Constructs a consumer group name based on the provided base group name and the enabled status of live and lane features.
     *
     * @param group the base consumer group name. If null, it will be treated as an empty string.
     * @return the constructed consumer group name with appended unit and lane information if applicable.
     */
    protected String getConsumerGroup(String group) {
        Location location = context.getLocation();
        group = group == null ? "" : group;
        Map<String, Object> map = new HashMap<>(3);
        map.put("group", group);
        if (context.isLiveEnabled()) {
            map.put("unit", location.getUnit());
        }
        if (context.isLaneEnabled()) {
            map.put("lane", location.getLane());
        }
        MqConfig config = context.getGovernanceConfig().getMqConfig();
        return config.getGroupTemplate().evaluate(map);
    }


    /**
     * Enum representing possible actions to take on a message.
     */
    protected enum MessageAction {
        /**
         * Consume the message.
         */
        CONSUME,

        /**
         * Discard the message.
         */
        DISCARD,

        /**
         * Reject the message.
         */
        REJECT
    }


}
