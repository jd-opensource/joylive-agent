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
import com.jd.live.agent.governance.policy.lane.LaneSpace;
import com.jd.live.agent.governance.policy.live.*;
import com.jd.live.agent.governance.policy.variable.UnitFunction;
import com.jd.live.agent.governance.request.Message;
import com.jd.live.agent.governance.request.Message.ProducerMessage;
import com.jd.live.agent.governance.request.MessageRoute;

/**
 * AbstractMessageInterceptor
 */
public abstract class AbstractMessageInterceptor extends InterceptorAdaptor {

    protected final InvocationContext context;

    protected final PolicySupplier policySupplier;

    protected final Location location;

    protected final MessageRoute messageRoute;

    protected final GovernanceConfig governanceConfig;

    public AbstractMessageInterceptor(InvocationContext context) {
        this.context = context;
        this.location = context.getLocation();
        this.messageRoute = context.getMessageRoute();
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
        LiveSpace liveSpace = policy.getCurrentLiveSpace();
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

    /**
     * Determines the target topic based on the given topic and the current context's location.
     *
     * @param topic the original topic to be evaluated.
     * @return the target topic after considering the unit and lane, or the original topic if no
     * features are enabled.
     */
    protected String getTarget(String topic) {
        String unit = isLiveEnabled(topic) ? location.getUnit() : null;
        String lane = isLaneEnabled(topic) ? location.getLane() : null;
        if (unit != null || lane != null) {
            return messageRoute.getTarget(topic, unit, lane);
        }
        return topic;
    }

    /**
     * Determines the target topic based on the given {@link Message} and the current context's location.
     *
     * @param message the {@link Message} from which to extract the topic and evaluate.
     * @return the target topic after considering the unit and lane, or the original topic if no
     *         features are enabled.
     */
    protected String getTarget(Message message) {
        String topic = message.getTopic();
        String unit = isLiveEnabled(topic) ? getTargetUnit(message) : null;
        String lane = isLaneEnabled(topic) ? getTargetLane(message) : null;
        if (unit != null || lane != null) {
            return messageRoute.getTarget(topic, unit, lane);
        }
        return topic;
    }

    /**
     * Converts a given topic to its source representation using the topic converter.
     *
     * @param topic the topic name to convert.
     * @return the source representation of the given topic.
     */
    protected String getSource(String topic) {
        return messageRoute.getSource(topic);
    }

    /**
     * Routes the given message to its target topic.
     *
     * @param message the {@link ProducerMessage} to be routed.
     */
    protected void route(ProducerMessage message) {
        message.setTopic(getTarget(message));
    }

    /**
     * Processes the given message by setting its topic and potentially adding a reply topic header.
     *
     * @param message the {@link ProducerMessage} to be processed.
     */
    protected void request(ProducerMessage message) {
        message.setTopic(getTarget(message));
    }


    /**
     * Determines if the given message is allowed to be consumed based on its topic and live/lane rules.
     *
     * @param message the {@link Message} to evaluate.
     * @return the {@link MessageAction} indicating whether to consume or discard the message.
     */
    protected MessageAction allow(Message message) {
        String topic = messageRoute.getSource(message.getTopic());
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
                return MessageAction.DISCARD;
            } else {
                UnitFunction func = context.getUnitFunction(rule.getVariableFunction());
                UnitRoute targetRoute = rule.getUnitRoute(message.getVariable(), func);
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
