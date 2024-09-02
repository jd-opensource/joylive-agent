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
import com.jd.live.agent.governance.config.GovernanceConfig;
import com.jd.live.agent.governance.config.LaneConfig;
import com.jd.live.agent.governance.config.LiveConfig;
import com.jd.live.agent.governance.invoke.InvocationContext;
import com.jd.live.agent.governance.policy.GovernancePolicy;
import com.jd.live.agent.governance.policy.PolicySupplier;
import com.jd.live.agent.governance.policy.lane.Lane;
import com.jd.live.agent.governance.policy.lane.LaneSpace;
import com.jd.live.agent.governance.policy.live.*;
import com.jd.live.agent.governance.policy.variable.UnitFunction;
import com.jd.live.agent.governance.request.Message;

/**
 * AbstractMessageInterceptor
 */
public abstract class AbstractMessageInterceptor extends InterceptorAdaptor {

    protected final InvocationContext context;

    protected final PolicySupplier policySupplier;

    protected final Location location;

    protected final GovernanceConfig governanceConfig;

    public AbstractMessageInterceptor(InvocationContext context) {
        this.context = context;
        this.location = context.getLocation();
        this.policySupplier = context.getPolicySupplier();
        this.governanceConfig = context.getGovernanceConfig();
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
        GovernancePolicy policy = policySupplier.getPolicy();
        LiveSpace liveSpace = policy == null ? null : policy.getCurrentLiveSpace();
        Unit local = liveSpace == null ? null : liveSpace.getCurrentUnit();
        return local == null || local.getAccessMode().isWriteable();
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
        return isLiveEnabled(topic) || isLaneEnabled(topic);
    }

    /**
     * Checks if the specified topic has live functionality enabled.
     *
     * @param topic the topic to check.
     * @return {@code true} if live functionality is enabled, otherwise {@code false}.
     */
    protected boolean isLiveEnabled(String topic) {
        LiveConfig liveConfig = governanceConfig.getLiveConfig();
        if (liveConfig.withTopic(topic)) {
            return true;
        }
        GovernancePolicy policy = policySupplier.getPolicy();
        LiveSpace liveSpace = policy == null ? null : policy.getLiveSpace(location.getLiveSpaceId());
        LiveSpec liveSpec = liveSpace == null ? null : liveSpace.getSpec();
        return liveSpec != null && liveSpec.withTopic(topic);
    }

    /**
     * Checks if the specified topic has lane functionality enabled.
     *
     * @param topic the topic to check.
     * @return {@code true} if lane functionality is enabled, otherwise {@code false}.
     */
    protected boolean isLaneEnabled(String topic) {
        LaneConfig laneConfig = governanceConfig.getLaneConfig();
        if (laneConfig.withTopic(topic)) {
            return true;
        }
        GovernancePolicy policy = policySupplier.getPolicy();
        LaneSpace laneSpace = policy == null ? null : policy.getLaneSpace(location.getLaneSpaceId());
        return laneSpace != null && laneSpace.withTopic(topic);
    }

    private boolean modifyGroupByLive(String topic) {
        if (!context.isLiveEnabled()) {
            return false;
        } else if (!governanceConfig.getLiveConfig().isModifyMQGroupEnabled()) {
            return false;
        } else if (topic != null && !topic.isEmpty()) {
            return isLiveEnabled(topic);
        }
        GovernancePolicy policy = policySupplier.getPolicy();
        LiveSpace space = policy == null ? null : policy.getCurrentLiveSpace();
        Unit unit = space == null ? null : space.getCurrentUnit();
        return unit != null;
    }

    private boolean modifyGroupByLane(String topic) {
        if (!context.isLaneEnabled()) {
            return false;
        } else if (!governanceConfig.getLaneConfig().isModifyMQGroupEnabled()) {
            return false;
        } else if (topic != null && !topic.isEmpty()) {
            return isLaneEnabled(topic);
        }
        GovernancePolicy policy = policySupplier.getPolicy();
        LaneSpace space = policy == null ? null : policy.getCurrentLaneSpace();
        Lane lane = space == null ? null : space.getCurrentLane();
        return lane != null;
    }

    protected String getGroup(String group, String topic) {
        String unit = modifyGroupByLive(topic) ? location.getUnit() : null;
        String lane = modifyGroupByLane(topic) ? location.getLane() : null;
        int unitLength = unit == null ? 0 : unit.length();
        int laneLength = lane == null ? 0 : lane.length();
        if (unitLength > 0 && laneLength > 0) {
            return group + "-unit-" + unit + "-lane-" + lane;
        } else if (unitLength > 0) {
            return group + "-unit-" + unit;
        } else if (laneLength > 0) {
            return group + "-lane-" + lane;
        } else {
            return group;
        }
    }

    /**
     * Determines if the given message is allowed to be consumed based on its topic and live/lane rules.
     *
     * @param message the {@link Message} to evaluate.
     * @return the {@link MessageAction} indicating whether to consume or discard the message.
     */
    protected MessageAction allow(Message message) {
        String topic = message.getTopic();
        if (!isEnabled(topic)) {
            return MessageAction.CONSUME;
        }
        MessageAction result = allowLive(message);
        return result == MessageAction.CONSUME ? allowLane(message) : result;
    }

    /**
     * Determines if the given message is allowed to be consumed based on live space rules.
     *
     * @param message the {@link Message} to evaluate.
     * @return the {@link MessageAction} indicating whether to consume or discard the message based on live space rules.
     */
    protected MessageAction allowLive(Message message) {
        String liveSpaceId = message.getLiveSpaceId();
        if (!context.isLiveEnabled()) {
            return MessageAction.CONSUME;
        } else if (liveSpaceId == null || liveSpaceId.isEmpty()) {
            return MessageAction.CONSUME;
        } else if (!liveSpaceId.equals(location.getLiveSpaceId())) {
            return MessageAction.DISCARD;
        } else {
            GovernancePolicy policy = policySupplier.getPolicy();
            LiveSpace liveSpace = policy == null ? null : policy.getLiveSpace(liveSpaceId);
            Unit local = liveSpace == null ? null : liveSpace.getCurrentUnit();
            UnitRule rule = liveSpace == null ? null : liveSpace.getUnitRule(message.getRuleId());
            if (liveSpace == null) {
                return MessageAction.CONSUME;
            } else if (local == null) {
                return MessageAction.DISCARD;
            } else if (rule == null) {
                return MessageAction.CONSUME;
            } else {
                UnitFunction func = context.getUnitFunction(rule.getVariableFunction());
                UnitRoute targetRoute = rule.getUnitRoute(message.getVariable(), func);
                Unit targetUnit = targetRoute == null ? null : targetRoute.getUnit();
                if (targetUnit == null) {
                    return MessageAction.CONSUME;
                } else if (targetUnit == local) {
                    return MessageAction.CONSUME;
                } else {
                    return local.getCode().equals(targetRoute.getFailoverUnit()) ? MessageAction.CONSUME : MessageAction.DISCARD;
                }
            }
        }
    }

    /**
     * Retrieves the target unit for the given message based on live space rules.
     *
     * @param message the {@link Message} to evaluate.
     * @return the target unit code, or null if no target unit is determined.
     */
    protected String getTargetUnit(Message message) {
        if (!context.isLiveEnabled()) {
            return null;
        } else {
            GovernancePolicy policy = policySupplier.getPolicy();
            LiveSpace liveSpace = policy == null ? null : policy.getLiveSpace(message.getLiveSpaceId());
            UnitRule rule = liveSpace == null ? null : liveSpace.getUnitRule(message.getRuleId());
            if (rule == null) {
                return null;
            } else {
                UnitFunction func = context.getUnitFunction(rule.getVariableFunction());
                UnitRoute route = rule.getUnitRoute(message.getVariable(), func);
                Unit unit = route == null ? null : route.getUnit();
                return unit == null ? null : unit.getCode();
            }
        }
    }

    /**
     * Determines if the given message is allowed to be consumed based on lane rules.
     *
     * @param message the {@link Message} to evaluate.
     * @return the {@link MessageAction} indicating whether to consume or discard the message based on lane rules.
     */
    protected MessageAction allowLane(Message message) {
        return location.inLane(message.getLaneSpaceId(), message.getLane())
                ? MessageAction.CONSUME
                : MessageAction.DISCARD;
    }

    /**
     * Retrieves the target lane for the given message.
     *
     * @param message the {@link Message} to evaluate.
     * @return the target lane.
     */
    protected String getTargetLane(Message message) {
        return message.getLane();
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
