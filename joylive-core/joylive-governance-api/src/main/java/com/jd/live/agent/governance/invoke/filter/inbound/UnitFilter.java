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
package com.jd.live.agent.governance.invoke.filter.inbound;

import com.jd.live.agent.core.extension.annotation.ConditionalOnProperty;
import com.jd.live.agent.core.extension.annotation.Extension;
import com.jd.live.agent.core.instance.GatewayRole;
import com.jd.live.agent.governance.config.GovernanceConfig;
import com.jd.live.agent.governance.context.RequestContext;
import com.jd.live.agent.governance.context.bag.Carrier;
import com.jd.live.agent.governance.invoke.InboundInvocation;
import com.jd.live.agent.governance.invoke.UnitAction;
import com.jd.live.agent.governance.invoke.UnitAction.UnitActionType;
import com.jd.live.agent.governance.invoke.filter.InboundFilter;
import com.jd.live.agent.governance.invoke.filter.InboundFilterChain;
import com.jd.live.agent.governance.invoke.metadata.LiveMetadata;
import com.jd.live.agent.governance.policy.live.*;
import com.jd.live.agent.governance.policy.service.live.UnitPolicy;
import com.jd.live.agent.governance.policy.variable.UnitFunction;
import com.jd.live.agent.governance.request.ServiceRequest.InboundRequest;

import java.util.concurrent.CompletionStage;

import static com.jd.live.agent.governance.invoke.Invocation.*;

/**
 * UnitFilter
 *
 * @author Zhiguo.Chen
 * @since 1.0.0
 */
@Extension(value = "UnitFilter", order = InboundFilter.ORDER_LIVE_UNIT)
@ConditionalOnProperty(value = GovernanceConfig.CONFIG_LIVE_ENABLED, matchIfMissing = true)
public class UnitFilter implements InboundFilter {

    @Override
    public <T extends InboundRequest> CompletionStage<Object> filter(InboundInvocation<T> invocation, InboundFilterChain chain) {
        UnitAction unitAction = unitAction(invocation);
        invocation.setUnitAction(unitAction);
        switch (unitAction.getType()) {
            case FAILOVER_CENTER:
            case FAILOVER:
                Carrier carrier = RequestContext.getOrCreate();
                carrier.setAttribute(Carrier.ATTRIBUTE_FAILOVER_UNIT, unitAction);
                break;
            case REJECT:
                invocation.reject(FaultType.UNIT, unitAction.getMessage());
                break;
            case FORWARD:
            default:
                break;
        }
        return chain.filter(invocation);
    }

    /**
     * Creates a unit action for the given inbound invocation.
     *
     * @param invocation the inbound invocation to create a unit action for
     * @return the created unit action
     */
    private <T extends InboundRequest> UnitAction unitAction(InboundInvocation<T> invocation) {
        UnitPolicy unitPolicy = invocation.getServiceMetadata().getUnitPolicy();
        LiveMetadata metadata = invocation.getLiveMetadata();
        UnitRule rule = metadata.getRule();
        String variable = metadata.getVariable();
        UnitAction action = validateSpace(invocation);
        if (action != null) {
            return action;
        } else if (metadata.isLocalLiveless()) {
            // liveless
            return onLiveless(invocation);
        } else if (rule == null) {
            return onMissingRule(invocation);
        } else if (metadata.getLocalUnit() == null) {
            return onMissingLocalUnit(invocation);
        } else if (unitPolicy == UnitPolicy.NONE) {
            return onNone(invocation);
        } else if (unitPolicy == UnitPolicy.CENTER) {
            return onCenter(invocation);
        } else if (unitPolicy == UnitPolicy.PREFER_LOCAL_UNIT) {
            return onPreferLocal(invocation);
        } else if (variable == null || variable.isEmpty()) {
            return onMissingVariable(invocation, rule);
        } else {
            return onRule(invocation, rule, variable);
        }
    }

    /**
     * Validates the live space for the given inbound invocation.
     *
     * @param invocation the inbound invocation
     * @return a UnitAction indicating the action to take, or null if the space is valid
     */
    private <T extends InboundRequest> UnitAction validateSpace(InboundInvocation<T> invocation) {
        LiveMetadata metadata = invocation.getLiveMetadata();
        String targetId = metadata.getTargetSpaceId();
        LiveSpace localSpace = metadata.getLocalSpace();

        if (invocation.getGateway() == GatewayRole.NONE && localSpace != null && !localSpace.getId().equals(targetId)) {
            // live space is not match
            return new UnitAction(UnitActionType.REJECT, invocation.getError(REJECT_NAMESPACE_NOT_MATCH));
        }
        return null;
    }

    /**
     * Handles the case when no other options are enabled.
     *
     * @param invocation the inbound invocation
     * @return a UnitAction indicating the action to take
     */
    private <T extends InboundRequest> UnitAction onLiveless(InboundInvocation<T> invocation) {
        return new UnitAction(UnitActionType.FORWARD);
    }

    /**
     * Handles the case when the local unit is missing.
     *
     * @param invocation the inbound invocation
     * @return a UnitAction indicating the action to take
     */
    private <T extends InboundRequest> UnitAction onMissingLocalUnit(InboundInvocation<T> invocation) {
        return new UnitAction(UnitActionType.REJECT, invocation.getError(REJECT_NO_UNIT));
    }

    /**
     * Handles the case when a rule is applied.
     *
     * @param invocation the inbound invocation
     * @param rule       the unit rule to apply
     * @param variable   the variable to check
     * @return a UnitAction indicating the action to take
     */
    private <T extends InboundRequest> UnitAction onRule(InboundInvocation<T> invocation, UnitRule rule, String variable) {
        Unit local = invocation.getLiveMetadata().getLocalUnit();
        UnitRoute unitRoute = rule.getUnitRoute(local.getCode());
        UnitFunction unitFunc = invocation.getContext().getUnitFunction(rule.getVariableFunction());
        if (!rule.contains(unitRoute, variable, unitFunc)) {
            return new UnitAction(UnitActionType.FAILOVER, invocation.getError(FAILOVER_UNIT_ESCAPE));
        }
        return invocation.isAccessible(local) ? new UnitAction(UnitActionType.FORWARD, null) :
                new UnitAction(UnitActionType.REJECT, invocation.getError(REJECT_UNIT_NOT_ACCESSIBLE));
    }

    /**
     * Handles the case when a variable is missing.
     *
     * @param invocation the inbound invocation
     * @param rule       the unit rule to apply
     * @return a UnitAction indicating the action to take
     */
    private <T extends InboundRequest> UnitAction onMissingVariable(InboundInvocation<T> invocation, UnitRule rule) {
        Unit local = invocation.getLiveMetadata().getLocalUnit();
        if (rule.getVariableMissingAction() == VariableMissingAction.CENTER) {
            if (local.getType() != UnitType.CENTER) {
                return new UnitAction(UnitActionType.FAILOVER_CENTER, invocation.getError(FAILOVER_CENTER_NO_VARIABLE));
            }
            return invocation.isAccessible(local) ? new UnitAction(UnitActionType.FORWARD, null) :
                    new UnitAction(UnitActionType.REJECT, invocation.getError(REJECT_UNIT_NOT_ACCESSIBLE));
        }
        return new UnitAction(UnitActionType.REJECT, invocation.getError(REJECT_NO_VARIABLE));
    }

    /**
     * Handles the case when the prefer local option is enabled.
     *
     * @param invocation the inbound invocation
     * @return a UnitAction indicating the action to take
     */
    private <T extends InboundRequest> UnitAction onPreferLocal(InboundInvocation<T> invocation) {
        LiveMetadata metadata = invocation.getLiveMetadata();
        Unit local = metadata.getLocalUnit();
        if (!invocation.isAccessible(local)) {
            Unit center = metadata.getLocalCenter();
            if (center != null && center != local && invocation.isAccessible(center)) {
                return new UnitAction(UnitActionType.FAILOVER_CENTER, invocation.getError(FAILOVER_UNIT_NOT_ACCESSIBLE));
            }
            return new UnitAction(UnitActionType.FAILOVER, invocation.getError(FAILOVER_UNIT_NOT_ACCESSIBLE));
        } else {
            return new UnitAction(UnitActionType.FORWARD, null);
        }
    }

    /**
     * Handles the case when the center option is enabled.
     *
     * @param invocation the inbound invocation
     * @return a UnitAction indicating the action to take
     */
    private <T extends InboundRequest> UnitAction onCenter(InboundInvocation<T> invocation) {
        Unit local = invocation.getLiveMetadata().getLocalUnit();
        if (local.getType() == UnitType.CENTER) {
            return invocation.isAccessible(local) ? new UnitAction(UnitActionType.FORWARD) :
                    new UnitAction(UnitActionType.REJECT, invocation.getError(FAILOVER_UNIT_NOT_ACCESSIBLE));
        } else {
            return new UnitAction(UnitActionType.FAILOVER_CENTER, invocation.getError(REJECT_UNIT_NOT_CENTER));
        }
    }

    /**
     * Handles the case when no other options are enabled.
     *
     * @param invocation the inbound invocation
     * @return a UnitAction indicating the action to take
     */
    private <T extends InboundRequest> UnitAction onNone(InboundInvocation<T> invocation) {
        Unit local = invocation.getLiveMetadata().getLocalUnit();
        return invocation.isAccessible(local) ? new UnitAction(UnitActionType.FORWARD) :
                new UnitAction(UnitActionType.FAILOVER, invocation.getError(FAILOVER_UNIT_NOT_ACCESSIBLE));
    }

    /**
     * Handles the case when a unit rule is missing.
     *
     * @param invocation the inbound invocation
     * @return a UnitAction indicating the action to take
     */
    private <T extends InboundRequest> UnitAction onMissingRule(InboundInvocation<T> invocation) {
        LiveMetadata metadata = invocation.getLiveMetadata();
        LiveSpace localSpace = metadata.getLocalSpace();
        if (localSpace == null || invocation.getGateway() != GatewayRole.NONE) {
            return new UnitAction(UnitActionType.FORWARD);
        }
        return new UnitAction(UnitActionType.REJECT, invocation.getError(REJECT_NO_UNIT_ROUTE));
    }

}
