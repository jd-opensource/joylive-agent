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
import com.jd.live.agent.governance.invoke.auth.Permission;
import com.jd.live.agent.governance.policy.AccessMode;
import com.jd.live.agent.governance.policy.GovernancePolicy;
import com.jd.live.agent.governance.policy.PolicySupplier;
import com.jd.live.agent.governance.policy.lane.Lane;
import com.jd.live.agent.governance.policy.live.*;
import com.jd.live.agent.governance.policy.live.db.LiveDatabase;
import com.jd.live.agent.governance.policy.variable.UnitFunction;
import com.jd.live.agent.governance.request.Message;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

import static com.jd.live.agent.core.util.StringUtils.*;

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
     * Checks consumption readiness for a topic from a single address string.
     *
     * @param topic the topic to check
     * @param address the cluster address string (comma-delimited if multiple)
     * @return Permission result from the delegated check
     */
    protected Permission isConsumeReady(String topic, String address) {
        return isReady(topic, split(address), address, mode -> mode.isReadable(), "readable");
    }

    /**
     * Checks if a topic can be produced to in the specified cluster.
     *
     * @param topic   topic name to check
     * @param address target cluster address
     * @return Permission result with success/failure and reason
     */
    protected Permission isProduceReady(String topic, String address) {
        return isReady(topic, split(address), address, mode -> mode.isWriteable(), "writeable");
    }

    /**
     * Evaluates whether the given topic operation is permitted under governance.
     *
     * @param topic     topic name under check
     * @param nodes     candidate cluster nodes (used when address is null)
     * @param address   resolved cluster address (may be null)
     * @param predicate access-mode predicate to validate (e.g., read/write)
     * @param message   human-readable access-mode description for denial messages
     * @return success when permitted; failure with denial reason otherwise
     */
    protected Permission isReady(String topic, String[] nodes, String address, Predicate<AccessMode> predicate, String message) {
        if (!context.isGovernReady()) {
            return Permission.failure("Application is not ready");
        } else if (!isEnabled(topic)) {
            return Permission.success();
        }
        GovernancePolicy policy = policySupplier.getPolicy();
        if (policy == null) {
            return Permission.success();
        }
        LiveSpace liveSpace = policy.getLocalLiveSpace();
        if (liveSpace == null) {
            return Permission.success();
        }
        LiveDatabase database = policy.getDatabase(nodes);
        if (database != null && !predicate.test(database.getAccessMode())) {
            return Permission.failure("MQ cluster is not " + message + ", cluster:" + (address == null ? join(nodes) : address));
        }
        Unit unit = database != null && !isEmpty(database.getUnit())
                ? liveSpace.getUnit(database.getUnit())
                : liveSpace.getLocalUnit();
        if (unit == null) {
            return Permission.success();
        }
        if (!predicate.test(unit.getAccessMode())) {
            return Permission.failure("Unit is not " + message + ", unit: " + unit.getCode());
        }
        Cell cell = database != null && !isEmpty(database.getCell())
                ? unit.getCell(database.getCell())
                : (unit == liveSpace.getLocalUnit() ? liveSpace.getLocalCell() : null);
        if (cell != null && !predicate.test(cell.getAccessMode())) {
            return Permission.failure("Cell is not " + message + ", unit: " + unit.getCode() + ", cell: " + cell.getCode());
        }
        return Permission.success();
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
        if (policy == null) {
            return group;
        }
        Unit unit = !context.isLiveEnabled() ? null : policy.getLocalUnit();
        Lane lane = !context.isLaneEnabled() ? null : policy.getLocalLane();
        if (unit == null && lane == null) {
            return group;
        }
        Map<String, String> map = new HashMap<>();
        map.put("group", group);
        map.put("unit", unit == null ? null : unit.getCode());
        map.put("lane", lane == null ? null : lane.getCode());
        map.put("topic", topic == null ? null : topic);
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
        if (!context.isLiveEnabled()) {
            return MessageAction.CONSUME;
        }
        MqMode mode = governanceConfig.getMqConfig().getLiveMode(message.getTopic());
        if (mode != MqMode.SHARED) {
            return MessageAction.CONSUME;
        }
        String targetLiveSpaceId = message.getLiveSpaceId();
        targetLiveSpaceId = targetLiveSpaceId == null || targetLiveSpaceId.isEmpty() ? null : message.getLocationLiveSpaceId();
        if (targetLiveSpaceId == null || targetLiveSpaceId.isEmpty()) {
            // not live message
            return MessageAction.CONSUME;
        }
        String targetUnitCode = message.getLocationUnit();
        String targetCellCode = message.getLocationCell();
        String localUnitCode = location.getUnit();
        String localCellCode = location.getCell();
        String localLiveSpaceId = location.getLiveSpaceId();
        GovernancePolicy policy = policySupplier.getPolicy();
        LiveSpace localLiveSpace = policy == null ? null : policy.getLocalLiveSpace();
        if (!targetLiveSpaceId.equals(localLiveSpaceId)) {
            return MessageAction.DISCARD;
        } else if (localUnitCode == null || localUnitCode.isEmpty()) {
            return MessageAction.DISCARD;
        } else if (localLiveSpace == null) {
            return MessageAction.DISCARD;
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
        if (!context.isLaneEnabled()) {
            return MessageAction.CONSUME;
        }
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
