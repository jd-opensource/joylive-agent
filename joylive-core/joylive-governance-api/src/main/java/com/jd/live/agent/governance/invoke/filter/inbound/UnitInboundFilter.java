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

import com.jd.live.agent.core.event.Publisher;
import com.jd.live.agent.core.extension.annotation.ConditionalOnProperty;
import com.jd.live.agent.core.extension.annotation.Extension;
import com.jd.live.agent.core.inject.annotation.Inject;
import com.jd.live.agent.core.inject.annotation.Injectable;
import com.jd.live.agent.governance.config.GovernanceConfig;
import com.jd.live.agent.governance.context.RequestContext;
import com.jd.live.agent.governance.context.bag.Carrier;
import com.jd.live.agent.governance.event.TrafficEvent;
import com.jd.live.agent.governance.event.TrafficEvent.ActionType;
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

import static com.jd.live.agent.governance.invoke.Invocation.*;

/**
 * UnitInboundFilter
 *
 * @author Zhiguo.Chen
 * @since 1.0.0
 */
@Injectable
@Extension(value = "UnitInboundFilter", order = InboundFilter.ORDER_INBOUND_LIVE_UNIT)
@ConditionalOnProperty(value = GovernanceConfig.CONFIG_LIVE_ENABLED, matchIfMissing = true)
public class UnitInboundFilter implements InboundFilter {

    @Inject(Publisher.TRAFFIC)
    private Publisher<TrafficEvent> publisher;

    @Override
    public <T extends InboundRequest> void filter(InboundInvocation<T> invocation, InboundFilterChain chain) {
        UnitAction unitAction = unitAction(invocation);
        invocation.setUnitAction(unitAction);
        switch (unitAction.getType()) {
            case FORWARD:
                chain.filter(invocation);
                break;
            case FAILOVER_CENTER:
            case FAILOVER:
                Carrier carrier = RequestContext.getOrCreate();
                carrier.setAttribute(Carrier.ATTRIBUTE_FAILOVER_UNIT, unitAction);
                chain.filter(invocation);
                break;
            case REJECT:
            case REJECT_ESCAPED:
                invocation.publish(publisher, TrafficEvent.builder().actionType(ActionType.REJECT).requests(1));
                invocation.reject(FaultType.UNIT, unitAction.getMessage());
        }
    }

    protected <T extends InboundRequest> UnitAction unitAction(InboundInvocation<T> invocation) {
        UnitPolicy unitPolicy = invocation.getServiceMetadata().getUnitPolicy();
        LiveMetadata liveMetadata = invocation.getLiveMetadata();
        UnitRule rule = liveMetadata.getUnitRule();
        String variable = liveMetadata.getVariable();
        Unit currentUnit = liveMetadata.getCurrentUnit();
        Unit center = liveMetadata.getCenterUnit();
        if (rule == null) {
            return new UnitAction(UnitActionType.FORWARD, null);
        } else if (unitPolicy == UnitPolicy.NONE) {
            return invocation.isAccessible(currentUnit) ? new UnitAction(UnitActionType.FORWARD, null) :
                    new UnitAction(UnitActionType.FAILOVER, invocation.getError(FAILOVER_UNIT_NOT_ACCESSIBLE));
        } else if (currentUnit == null) {
            return new UnitAction(UnitActionType.REJECT, invocation.getError(REJECT_NO_UNIT));
        } else if (unitPolicy == UnitPolicy.CENTER) {
            if (currentUnit.getType() == UnitType.CENTER) {
                return invocation.isAccessible(currentUnit) ? new UnitAction(UnitActionType.FORWARD, null) :
                        new UnitAction(UnitActionType.REJECT, invocation.getError(FAILOVER_UNIT_NOT_ACCESSIBLE));
            } else {
                return new UnitAction(UnitActionType.FAILOVER_CENTER, invocation.getError(REJECT_UNIT_NOT_CENTER));
            }
        } else if (unitPolicy == UnitPolicy.PREFER_LOCAL_UNIT) {
            if (!invocation.isAccessible(currentUnit)) {
                if (center != null && center != currentUnit && invocation.isAccessible(center)) {
                    return new UnitAction(UnitActionType.FAILOVER_CENTER, invocation.getError(FAILOVER_UNIT_NOT_ACCESSIBLE));
                }
                return new UnitAction(UnitActionType.FAILOVER, invocation.getError(FAILOVER_UNIT_NOT_ACCESSIBLE));
            } else {
                return new UnitAction(UnitActionType.FORWARD, null);
            }
        } else if (variable == null || variable.isEmpty()) {
            if (rule.getVariableMissingAction() == VariableMissingAction.CENTER) {
                if (currentUnit.getType() != UnitType.CENTER) {
                    return new UnitAction(UnitActionType.FAILOVER_CENTER, invocation.getError(FAILOVER_CENTER_NO_VARIABLE));
                }
                return invocation.isAccessible(currentUnit) ? new UnitAction(UnitActionType.FORWARD, null) :
                        new UnitAction(UnitActionType.REJECT, invocation.getError(REJECT_UNIT_NOT_ACCESSIBLE));
            }
            return new UnitAction(UnitActionType.REJECT, invocation.getError(REJECT_NO_VARIABLE));
        } else {
            UnitRoute unitRoute = rule.getUnitRoute(currentUnit.getCode());
            UnitFunction unitFunc = invocation.getContext().getUnitFunction(rule.getVariableFunction());
            if (!rule.contains(unitRoute, variable, unitFunc)) {
                return new UnitAction(UnitActionType.FAILOVER, invocation.getError(FAILOVER_ESCAPE));
            }
            return invocation.isAccessible(currentUnit) ? new UnitAction(UnitActionType.FORWARD, null) :
                    new UnitAction(UnitActionType.REJECT, invocation.getError(REJECT_UNIT_NOT_ACCESSIBLE));
        }
    }

}
