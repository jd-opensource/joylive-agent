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
import com.jd.live.agent.core.extension.annotation.Extension;
import com.jd.live.agent.core.inject.annotation.Inject;
import com.jd.live.agent.core.inject.annotation.Injectable;
import com.jd.live.agent.governance.event.TrafficEvent;
import com.jd.live.agent.governance.event.TrafficEvent.ActionType;
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
@Injectable
@Extension(value = "FailoverInboundFilter", order = InboundFilter.ORDER_INBOUND_LIVE_FAILOVER)
public class FailoverInboundFilter implements InboundFilter {

    @Inject(Publisher.TRAFFIC)
    private Publisher<TrafficEvent> publisher;

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
        invocation.publish(publisher, TrafficEvent.builder().actionType(ActionType.FORWARD).requests(1));
        chain.filter(invocation);
    }

    protected <T extends InboundRequest> void failoverUnit(InboundInvocation<T> invocation, UnitAction unitAction) {
        invocation.publish(publisher, TrafficEvent.builder().actionType(ActionType.REJECT).requests(1));
        invocation.failover(FaultType.UNIT, unitAction.getMessage());
    }

    protected <T extends InboundRequest> void failoverCell(InboundInvocation<T> invocation, CellAction cellAction) {
        invocation.publish(publisher, TrafficEvent.builder().actionType(ActionType.REJECT).requests(1));
        invocation.failover(FaultType.CELL, cellAction.getMessage());
    }

}
