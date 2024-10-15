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
import com.jd.live.agent.governance.config.GovernanceConfig;
import com.jd.live.agent.governance.invoke.CellAction;
import com.jd.live.agent.governance.invoke.InboundInvocation;
import com.jd.live.agent.governance.invoke.UnitAction;
import com.jd.live.agent.governance.invoke.filter.InboundFilter;
import com.jd.live.agent.governance.invoke.filter.InboundFilterChain;
import com.jd.live.agent.governance.policy.live.FaultType;
import com.jd.live.agent.governance.request.ServiceRequest.InboundRequest;

/**
 * FailoverInboundFilter
 *
 * @author Zhiguo.Chen
 * @since 1.0.0
 */
@Extension(value = "FailoverInboundFilter", order = InboundFilter.ORDER_LIVE_FAILOVER)
@ConditionalOnProperty(value = GovernanceConfig.CONFIG_LIVE_ENABLED, matchIfMissing = true)
public class FailoverInboundFilter implements InboundFilter {

    @Override
    public <T extends InboundRequest> void filter(InboundInvocation<T> invocation, InboundFilterChain chain) {
        UnitAction unitAction = invocation.getUnitAction();
        switch (unitAction.getType()) {
            case FAILOVER:
            case FAILOVER_CENTER:
                failoverUnit(invocation, unitAction);
                return;
        }
        CellAction cellAction = invocation.getCellAction();
        if (cellAction.getType() == CellAction.CellActionType.FAILOVER) {
            failoverCell(invocation, cellAction);
            return;
        }
        chain.filter(invocation);
    }

    protected <T extends InboundRequest> void failoverUnit(InboundInvocation<T> invocation, UnitAction unitAction) {
        invocation.failover(FaultType.UNIT, unitAction.getMessage());
    }

    protected <T extends InboundRequest> void failoverCell(InboundInvocation<T> invocation, CellAction cellAction) {
        invocation.failover(FaultType.CELL, cellAction.getMessage());
    }

}
