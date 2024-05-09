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
import com.jd.live.agent.core.extension.annotation.Extension;
import com.jd.live.agent.core.util.network.Ipv4;
import com.jd.live.agent.governance.config.GovernanceConfig;
import com.jd.live.agent.governance.invoke.OutboundInvocation;
import com.jd.live.agent.governance.invoke.RouteTarget;
import com.jd.live.agent.governance.invoke.filter.RouteFilter;
import com.jd.live.agent.governance.invoke.filter.RouteFilterChain;
import com.jd.live.agent.governance.request.ServiceRequest.OutboundRequest;

/**
 * LocalhostFilter is a debug-mode filter that restricts the route targets to only those
 * running on the localhost. This filter is useful for testing and debugging purposes, as
 * it ensures that requests are only routed to local instances of the service.
 */
@Extension(value = "LocalhostFilter", order = RouteFilter.ORDER_LOCALHOST)
@ConditionalOnProperty(GovernanceConfig.CONFIG_LOCALHOST_ENABLED)
public class LocalhostFilter implements RouteFilter {

    @Override
    public <T extends OutboundRequest> void filter(OutboundInvocation<T> invocation, RouteFilterChain chain) {
        RouteTarget target = invocation.getRouteTarget();
        String localIp = Ipv4.getLocalIp();
        if (localIp != null) {
            target.filter(endpoint -> endpoint.getHost().equals(localIp));
        }
        chain.filter(invocation);
    }
}
