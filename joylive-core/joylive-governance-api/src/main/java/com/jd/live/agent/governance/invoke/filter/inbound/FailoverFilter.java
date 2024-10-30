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
import com.jd.live.agent.governance.invoke.CellAction.CellActionType;
import com.jd.live.agent.governance.invoke.InboundInvocation;
import com.jd.live.agent.governance.invoke.UnitAction;
import com.jd.live.agent.governance.invoke.UnitAction.UnitActionType;
import com.jd.live.agent.governance.invoke.filter.InboundFilter;
import com.jd.live.agent.governance.invoke.filter.InboundFilterChain;
import com.jd.live.agent.governance.policy.live.FaultType;
import com.jd.live.agent.governance.request.ServiceRequest.InboundRequest;

import java.util.concurrent.CompletionStage;

/**
 * FailoverFilter
 *
 * @author Zhiguo.Chen
 * @since 1.0.0
 */
@Extension(value = "FailoverFilter", order = InboundFilter.ORDER_LIVE_FAILOVER)
@ConditionalOnProperty(value = GovernanceConfig.CONFIG_LIVE_ENABLED, matchIfMissing = true)
public class FailoverFilter implements InboundFilter {

    @Override
    public <T extends InboundRequest> CompletionStage<Object> filter(InboundInvocation<T> invocation, InboundFilterChain chain) {
        UnitAction unitAction = invocation.getUnitAction();
        CellAction cellAction = invocation.getCellAction();
        if (unitAction.getType() == UnitActionType.FAILOVER || unitAction.getType() == UnitActionType.FAILOVER_CENTER) {
            invocation.failover(FaultType.UNIT, unitAction.getMessage());
        } else if (cellAction.getType() == CellActionType.FAILOVER) {
            invocation.failover(FaultType.CELL, cellAction.getMessage());
        }
        return chain.filter(invocation);
    }

}
