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
package com.jd.live.agent.governance.invoke.filter.route;

import com.jd.live.agent.core.extension.annotation.ConditionalOnProperty;
import com.jd.live.agent.core.extension.annotation.ConditionalRelation;
import com.jd.live.agent.core.extension.annotation.Extension;
import com.jd.live.agent.core.inject.annotation.Inject;
import com.jd.live.agent.core.inject.annotation.Injectable;
import com.jd.live.agent.core.instance.AppStatus;
import com.jd.live.agent.core.instance.Application;
import com.jd.live.agent.governance.config.GovernanceConfig;
import com.jd.live.agent.governance.invoke.OutboundInvocation;
import com.jd.live.agent.governance.invoke.filter.RouteFilter;
import com.jd.live.agent.governance.invoke.filter.RouteFilterChain;
import com.jd.live.agent.governance.policy.live.FaultType;
import com.jd.live.agent.governance.request.ServiceRequest.OutboundRequest;

/**
 * A route filter that checks if the service instance is ready before allowing outbound requests.
 *
 * @see GovernanceConfig
 * @see Application
 */
@ConditionalOnProperty(name = {
        GovernanceConfig.CONFIG_FLOW_CONTROL_ENABLED,
        GovernanceConfig.CONFIG_LIVE_ENABLED,
        GovernanceConfig.CONFIG_LANE_ENABLED
}, matchIfMissing = true, relation = ConditionalRelation.OR)
@Injectable
@Extension(value = "ReadyFilter", order = RouteFilter.ORDER_READY)
public class ReadyFilter implements RouteFilter {

    @Inject(Application.COMPONENT_APPLICATION)
    private Application application;

    @Override
    public <T extends OutboundRequest> void filter(OutboundInvocation<T> invocation, RouteFilterChain chain) {
        AppStatus status = application.getStatus();
        if (!status.outbound()) {
            invocation.reject(FaultType.UNREADY, status.getMessage());
        }
        chain.filter(invocation);
    }
}
