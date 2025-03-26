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

import com.jd.live.agent.core.extension.annotation.Extension;
import com.jd.live.agent.core.instance.Application;
import com.jd.live.agent.core.instance.Location;
import com.jd.live.agent.governance.annotation.ConditionalOnLiveEnabled;
import com.jd.live.agent.governance.context.bag.Carrier;
import com.jd.live.agent.governance.invoke.CellAction;
import com.jd.live.agent.governance.invoke.CellAction.CellActionType;
import com.jd.live.agent.governance.invoke.InboundInvocation;
import com.jd.live.agent.governance.invoke.UnitAction;
import com.jd.live.agent.governance.invoke.UnitAction.UnitActionType;
import com.jd.live.agent.governance.invoke.filter.InboundFilter;
import com.jd.live.agent.governance.invoke.filter.InboundFilterChain;
import com.jd.live.agent.governance.invoke.metadata.LiveMetadata;
import com.jd.live.agent.governance.policy.live.*;
import com.jd.live.agent.governance.request.ServiceRequest.InboundRequest;

import java.util.concurrent.CompletionStage;

import static com.jd.live.agent.core.util.StringUtils.isEmpty;
import static com.jd.live.agent.governance.invoke.Invocation.FAILOVER_CELL_ESCAPE;
import static com.jd.live.agent.governance.invoke.Invocation.FAILOVER_CELL_NOT_ACCESSIBLE;

/**
 * CellFilter
 *
 * @author Zhiguo.Chen
 * @since 1.0.0
 */
@Extension(value = "CellFilter", order = InboundFilter.ORDER_LIVE_CELL)
@ConditionalOnLiveEnabled
public class CellFilter implements InboundFilter {

    @Override
    public <T extends InboundRequest> CompletionStage<Object> filter(InboundInvocation<T> invocation, InboundFilterChain chain) {
        UnitAction unitAction = invocation.getUnitAction();
        if (unitAction.getType() == UnitActionType.FORWARD) {
            CellAction cellAction = cellAction(invocation);
            invocation.setCellAction(cellAction);
            if (cellAction.getType() == CellActionType.FAILOVER) {
                Carrier carrier = invocation.getRequest().getOrCreateCarrier();
                carrier.setAttribute(Carrier.ATTRIBUTE_FAILOVER_CELL, cellAction);
                Application application = invocation.getContext().getApplication();
                if (application.getService().isGateway()) {
                    // gateway inbound request
                    invocation.setCellAction(new CellAction(CellActionType.FORWARD));
                }
            }
        }
        return chain.filter(invocation);
    }

    protected <T extends InboundRequest> CellAction cellAction(InboundInvocation<T> invocation) {
        LiveMetadata metadata = invocation.getLiveMetadata();
        if (metadata.isLocalLiveless()) {
            // liveless
            return new CellAction(CellActionType.FORWARD);
        }
        UnitRule rule = metadata.getRule();
        if (rule == null) {
            return new CellAction(CellActionType.FORWARD);
        }
        Unit localUnit = metadata.getLocalUnit();
        Cell localCell = metadata.getLocalCell();
        String variable = metadata.getVariable();
        UnitRoute unitRoute;
        CellRoute cellRoute = null;
        Location location = invocation.getContext().getApplication().getLocation();
        if (localUnit == null) {
            // one unit.
            unitRoute = rule.size() == 1 && isEmpty(location.getUnit()) ? rule.getUnitRoutes().get(0) : null;
        } else {
            unitRoute = rule.getUnitRoute(localUnit.getCode());
        }
        if (unitRoute != null) {
            // allow list
            cellRoute = unitRoute.getCellRouteByVariable(variable);
            if (cellRoute == null) {
                // one cell
                if (localCell == null && unitRoute.size() == 1 && isEmpty(location.getCell())) {
                    cellRoute = unitRoute.getCells().get(0);
                    localCell = cellRoute.getCell();
                } else if (localCell != null) {
                    cellRoute = unitRoute.getCellRoute(localCell.getCode());
                }
            } else if (cellRoute.getCell() != localCell) {
                return new CellAction(CellActionType.FAILOVER, invocation.getError(FAILOVER_CELL_ESCAPE));
            }
        }
        if (invocation.isAccessible(localCell) && (cellRoute == null
                || !cellRoute.isEmpty() && invocation.isAccessible(cellRoute.getAccessMode()))) {
            return new CellAction(CellActionType.FORWARD);
        }
        return new CellAction(CellActionType.FAILOVER, invocation.getError(FAILOVER_CELL_NOT_ACCESSIBLE));
    }

}
