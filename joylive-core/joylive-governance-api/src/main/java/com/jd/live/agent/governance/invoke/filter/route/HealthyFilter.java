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
import com.jd.live.agent.governance.instance.Endpoint;
import com.jd.live.agent.governance.invoke.OutboundInvocation;
import com.jd.live.agent.governance.invoke.RouteTarget.MinPercentPredicate;
import com.jd.live.agent.governance.invoke.filter.ConstraintRouteFilter;
import com.jd.live.agent.governance.invoke.filter.RouteFilter;
import com.jd.live.agent.governance.policy.service.ServicePolicy;
import com.jd.live.agent.governance.policy.service.health.HealthPolicy;
import com.jd.live.agent.governance.request.ServiceRequest.OutboundRequest;

/**
 * A filter that removes unhealthy instances from the list of route targets. This filter
 * is applied during the routing process to ensure that only instances in a healthy or
 * acceptable state are considered for routing requests.
 *
 * @since 1.0.0
 */
@Extension(value = "HealthyFilter", order = RouteFilter.ORDER_HEALTH)
@ConditionalOnFlowControlEnabled
public class HealthyFilter implements ConstraintRouteFilter {

    @Override
    public <T extends OutboundRequest> Constraint getConstraint(OutboundInvocation<T> invocation) {
        ServicePolicy servicePolicy = invocation.getServiceMetadata().getServicePolicy();
        HealthPolicy healthPolicy = servicePolicy == null ? null : servicePolicy.getHealthPolicy();
        int healthyMinPercent = healthPolicy == null ? 0 : healthPolicy.getHealthyMinPercent(0);
        if (healthyMinPercent <= 0) {
            return new Constraint(Endpoint::isAccessible);
        } else {
            return new Constraint(Endpoint::isAccessible, -1, new MinPercentPredicate(healthyMinPercent));
        }
    }
}
