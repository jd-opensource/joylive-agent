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

import com.jd.live.agent.bootstrap.logger.Logger;
import com.jd.live.agent.bootstrap.logger.LoggerFactory;
import com.jd.live.agent.core.extension.annotation.Extension;
import com.jd.live.agent.governance.annotation.ConditionalOnFlowControlEnabled;
import com.jd.live.agent.governance.invoke.OutboundInvocation;
import com.jd.live.agent.governance.invoke.RouteTarget;
import com.jd.live.agent.governance.invoke.filter.RouteFilter;
import com.jd.live.agent.governance.invoke.filter.RouteFilterChain;
import com.jd.live.agent.governance.request.Portable;
import com.jd.live.agent.governance.request.ServiceRequest.OutboundRequest;

/**
 * A class that implements the {@link RouteFilter} interface.
 * It filters outbound requests based on the port number.
 *
 * @since 1.6.0
 */
@Extension(value = "PortFilter", order = RouteFilter.ORDER_PORT)
@ConditionalOnFlowControlEnabled
public class PortFilter implements RouteFilter {

    private static final Logger logger = LoggerFactory.getLogger(PortFilter.class);

    @Override
    public <T extends OutboundRequest> void filter(OutboundInvocation<T> invocation, RouteFilterChain chain) {
        T request = invocation.getRequest();
        RouteTarget target = invocation.getRouteTarget();
        if (request instanceof Portable) {
            Integer port = ((Portable) request).getPort();
            if (port != null) {
                invocation.getRouteTarget().filter(e -> e.isPort(port));
            }
        }
        if (logger.isDebugEnabled()) {
            logger.debug("Port filter applied to route target instance size: {}", target.size());
        }
        chain.filter(invocation);
    }
}
