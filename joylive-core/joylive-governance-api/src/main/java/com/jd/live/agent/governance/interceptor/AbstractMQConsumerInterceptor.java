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
import com.jd.live.agent.governance.policy.lane.LaneSpace;
import com.jd.live.agent.governance.policy.live.*;
import com.jd.live.agent.governance.policy.mq.MqPolicy;
import com.jd.live.agent.governance.policy.variable.UnitFunction;

/**
 * AbstractMQConsumerInterceptor
 */
public abstract class AbstractMQConsumerInterceptor extends InterceptorAdaptor {

    protected final InvocationContext context;

    public AbstractMQConsumerInterceptor(InvocationContext context) {
        this.context = context;
    }

    /**
     * Determines if the given topic is ready to be consumed based on the governance context and policies.
     *
     * @param topic the topic name to check
     * @return {@code true} if the topic is ready to be consumed, {@code false} otherwise
     */
    protected boolean isConsumeReady(String topic) {
        if (!context.isGovernReady()) {
            return false;
        }
        if (!isEnabled(topic)) {
            return true;
        }
        GovernancePolicy policy = context.getPolicySupplier().getPolicy();
        LiveSpace liveSpace = policy.getCurrentLiveSpace();
        Unit local = liveSpace == null ? null : liveSpace.getCurrentUnit();
        return local != null && !local.getAccessMode().isWriteable();
    }

    /**
     * Determines if the given topic is enabled based on the current governance policies and location.
     *
     * @param topic the topic name to check
     * @return {@code true} if the topic is enabled, {@code false} otherwise
     */
    protected boolean isEnabled(String topic) {
        if (topic == null || topic.isEmpty()) {
            return false;
        }
        GovernancePolicy policy = context.getPolicySupplier().getPolicy();
        Location location = context.getLocation();
        LiveSpace liveSpace = policy == null ? null : policy.getLiveSpace(location.getLiveSpaceId());
        LiveSpec liveSpec = liveSpace == null ? null : liveSpace.getSpec();
        MqPolicy mqPolicy = liveSpec == null ? null : liveSpec.getMqPolicy();
        if (mqPolicy == null || !mqPolicy.include(topic)) {
            LaneSpace laneSpace = policy == null ? null : policy.getLaneSpace(location.getLaneSpaceId());
            mqPolicy = laneSpace == null ? null : laneSpace.getMqPolicy();
            return mqPolicy != null && mqPolicy.include(topic);
        }
        return true;
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
