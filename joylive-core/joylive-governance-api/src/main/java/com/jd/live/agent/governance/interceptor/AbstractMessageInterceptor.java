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
import com.jd.live.agent.core.util.template.Evaluator;
import com.jd.live.agent.governance.config.GovernanceConfig;
import com.jd.live.agent.governance.config.MqMode;
import com.jd.live.agent.governance.invoke.InvocationContext;
import com.jd.live.agent.governance.policy.GovernancePolicy;
import com.jd.live.agent.governance.policy.PolicySupplier;
import com.jd.live.agent.governance.policy.lane.Lane;
import com.jd.live.agent.governance.policy.lane.LaneSpace;
import com.jd.live.agent.governance.policy.live.*;
import com.jd.live.agent.governance.policy.variable.UnitFunction;
import com.jd.live.agent.governance.request.Message;

import java.util.HashMap;
import java.util.Map;

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
        } else if (!isEnabled(topic)) {
            return true;
        }
        GovernancePolicy policy = policySupplier.getPolicy();
        LiveSpace liveSpace = policy == null ? null : policy.getLocalLiveSpace();
        Unit unit = liveSpace == null ? null : liveSpace.getLocalUnit();
        Cell cell = liveSpace == null ? null : liveSpace.getLocalCell();
        if (unit == null) {
            return true;
        } else if (!unit.getAccessMode().isReadable()) {
            return false;
        }
        return cell == null || cell.getAccessMode().isReadable();
    }

    /**
     * Determines if the given topic is enabled based on the current governance policies and location.
     *
     * @param topic the topic name to check
     * @return {@code true} if the topic is enabled, {@code false} otherwise
     */
    protected boolean isEnabled(String topic) {
        return governanceConfig.getMqConfig().isEnabled(topic);
    }

    /**
     * Generates a dynamic group name based on the provided group template and topic context.
     *
     * @param group The base group name to use as fallback (never null)
     * @param topic The topic name used to determine custom template configuration (may be null)
     * @return The rendered group name if successful, otherwise the original group name
     */
    protected String getGroup(String group, String topic) {
        Evaluator evaluator = governanceConfig.getMqConfig().getGroupTemplate(topic);
        if (evaluator == null) {
            return group;
        }
        GovernancePolicy policy = policySupplier.getPolicy();
        Unit unit = null;
        if (context.isLiveEnabled()) {
            LiveSpace space = policy == null ? null : policy.getLocalLiveSpace();
            unit = space == null ? null : space.getLocalUnit();
        }
        Lane lane = null;
        if (context.isLaneEnabled()) {
            LaneSpace space = policy == null ? null : policy.getLocalLaneSpace();
            lane = space == null ? null : space.getCurrentLane();
        }
        if (unit == null || lane == null) {
            return group;
        }
        Map<String, String> map = new HashMap<>();
        map.put("group", group);
        map.put("unit", unit.getCode());
        map.put("lane", lane.getCode());
        if (topic != null && !topic.isEmpty()) {
            map.put("topic", topic);
        }
        String result = evaluator.render(map);
        return result == null || result.isEmpty() ? group : result;
    }

    /**
     * Determines if the given message is allowed to be consumed based on its topic and live/lane rules.
     *
     * @param message the {@link Message} to evaluate.
     * @return the {@link MessageAction} indicating whether to consume or discard the message.
     */
    protected MessageAction consume(Message message) {
        String topic = message.getTopic();
        if (!isEnabled(topic)) {
            return MessageAction.CONSUME;
        }
        MessageAction result = consumeLive(message);
        return result == MessageAction.CONSUME ? consumeLane(message) : result;
    }

    /**
     * Determines if the given message is allowed to be consumed based on live space rules.
     *
     * @param message the {@link Message} to evaluate.
     * @return the {@link MessageAction} indicating whether to consume or discard the message based on live space rules.
     */
    protected MessageAction consumeLive(Message message) {
        GovernancePolicy policy = policySupplier.getPolicy();
        String targetLiveSpaceId = message.getLiveSpaceId();
        targetLiveSpaceId = targetLiveSpaceId == null || targetLiveSpaceId.isEmpty() ? null : message.getLocationLiveSpaceId();
        String targetUnitCode = message.getLocationUnit();
        String targetCellCode = message.getLocationCell();
        String localUnitCode = location.getUnit();
        String localCellCode = location.getCell();
        String localLiveSpaceId = location.getLiveSpaceId();
        LiveSpace localLiveSpace = policy == null ? null : policy.getLocalLiveSpace();
        MqMode mode = governanceConfig.getMqConfig().getLiveMode(message.getTopic());
        if (mode != MqMode.SHARED) {
            return MessageAction.CONSUME;
        } else if (!context.isLiveEnabled()) {
            return MessageAction.CONSUME;
        } else if (targetLiveSpaceId == null || targetLiveSpaceId.isEmpty()) {
            return MessageAction.CONSUME;
        } else if (!targetLiveSpaceId.equals(localLiveSpaceId)) {
            return MessageAction.DISCARD;
        } else if (localUnitCode == null || localUnitCode.isEmpty()) {
            return MessageAction.DISCARD;
        } else if (localLiveSpace == null) {
            return MessageAction.CONSUME;
        } else {
            String targetFailoverUnitCode = null;
            Unit localUnit = localLiveSpace.getLocalUnit();
            Cell localCell = localLiveSpace.getLocalCell();
            Unit targetUnit = localLiveSpace.getUnit(targetUnitCode);
            Cell targetCell = targetUnit == null ? null : targetUnit.getCell(targetCellCode);
            CellRoute cellRoute = null;
            UnitRule rule = localLiveSpace.getUnitRule(message.getRuleId());
            if (rule != null) {
                UnitFunction func = context.getUnitFunction(rule.getVariableFunction());
                UnitRoute targetRoute = rule.getUnitRoute(message.getVariable(), func);
                cellRoute = targetRoute == null ? null : targetRoute.getCellRoute(targetCellCode);
                targetUnit = targetRoute == null ? targetUnit : targetRoute.getUnit();
                targetCell = cellRoute == null ? targetCell : cellRoute.getCell();
                targetUnitCode = targetUnit == null ? targetUnitCode : targetUnit.getCode();
                targetFailoverUnitCode = targetRoute == null ? null : targetRoute.getFailoverUnit();
            }
            if (!localUnitCode.equals(targetUnitCode) && !localUnitCode.equals(targetFailoverUnitCode)) {
                return MessageAction.DISCARD;
            } else if (!localUnit.getAccessMode().isReadable()) {
                return MessageAction.REJECT;
            } else if (localCellCode != null && !localCellCode.isEmpty() && !localCellCode.equals(targetCellCode)) {
                if (targetCell != null && targetCell.getAccessMode().isReadable()) {
                    return MessageAction.DISCARD;
                } else if (cellRoute != null && cellRoute.getAccessMode().isReadable()) {
                    return MessageAction.DISCARD;
                }
            }
            if (localCell != null && !localCell.getAccessMode().isReadable()) {
                return MessageAction.REJECT;
            }
            return MessageAction.CONSUME;
        }
    }

    /**
     * Determines if the given message is allowed to be consumed based on lane rules.
     *
     * @param message the {@link Message} to evaluate.
     * @return the {@link MessageAction} indicating whether to consume or discard the message based on lane rules.
     */
    protected MessageAction consumeLane(Message message) {
        MqMode mode = governanceConfig.getMqConfig().getLaneMode(message.getTopic());
        if (mode != MqMode.SHARED) {
            return MessageAction.CONSUME;
        }
        return location.inLane(message.getLaneSpaceId(), message.getLane())
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
