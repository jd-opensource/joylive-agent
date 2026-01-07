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

import com.jd.live.agent.core.extension.annotation.Extension;
import com.jd.live.agent.governance.annotation.ConditionalOnFlowControlEnabled;
import com.jd.live.agent.governance.config.ServiceConfig;
import com.jd.live.agent.governance.invoke.OutboundInvocation;
import com.jd.live.agent.governance.invoke.filter.ConstraintRouteFilter;
import com.jd.live.agent.governance.invoke.filter.RouteFilter;
import com.jd.live.agent.governance.invoke.metadata.ServiceMetadata;
import com.jd.live.agent.governance.request.ServiceRequest.OutboundRequest;

/**
 * Support service calls by configuring to filter instances of a specific group
 *
 * @since 1.4.0
 */
@Extension(value = "GroupFilter", order = RouteFilter.ORDER_GROUP)
@ConditionalOnFlowControlEnabled
public class GroupFilter implements ConstraintRouteFilter {

    @Override
    public <T extends OutboundRequest> Constraint getConstraint(OutboundInvocation<T> invocation) {
        if (invocation.getRequest().isNativeGroup()) {
            return null;
        }
        ServiceMetadata serviceMetadata = invocation.getServiceMetadata();
        String group = serviceMetadata.getServiceGroup();
        if (group != null && !group.isEmpty()) {
            return new Constraint(e -> e.isGroup(group));
        }
        ServiceConfig serviceConfig = serviceMetadata.getServiceConfig();
        if (serviceConfig != null && !serviceConfig.isServiceGroupOpen()) {
            // default group
            return new Constraint(e -> e.isGroup(null));
        }
        return null;
    }
}
