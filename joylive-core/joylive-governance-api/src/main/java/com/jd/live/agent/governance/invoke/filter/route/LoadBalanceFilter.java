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
import com.jd.live.agent.governance.context.RequestContext;
import com.jd.live.agent.governance.instance.Endpoint;
import com.jd.live.agent.governance.invoke.OutboundInvocation;
import com.jd.live.agent.governance.invoke.RouteTarget;
import com.jd.live.agent.governance.invoke.filter.RouteFilter;
import com.jd.live.agent.governance.invoke.filter.RouteFilterChain;
import com.jd.live.agent.governance.invoke.loadbalance.Candidate;
import com.jd.live.agent.governance.invoke.loadbalance.LoadBalancer;
import com.jd.live.agent.governance.policy.service.ServicePolicy;
import com.jd.live.agent.governance.policy.service.loadbalance.LoadBalancePolicy;
import com.jd.live.agent.governance.request.Request;
import com.jd.live.agent.governance.request.ServiceRequest.OutboundRequest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * LoadBalanceFilter applies load balancing to the list of route targets. It ensures that
 * requests are distributed across available instances in a balanced manner based on the
 * configured load balancing policy.
 *
 * @since 1.0.0
 */
@Extension(value = "LoadBalanceFilter", order = RouteFilter.ORDER_LOAD_BALANCE)
@ConditionalOnFlowControlEnabled
public class LoadBalanceFilter implements RouteFilter {

    private static final Logger logger = LoggerFactory.getLogger(LoadBalanceFilter.class);

    @Override
    public <T extends OutboundRequest> void filter(OutboundInvocation<T> invocation, RouteFilterChain chain) {
        RouteTarget target = invocation.getRouteTarget();
        if (!target.isEmpty()) {
            List<? extends Endpoint> prefers = preferSticky(target, invocation);
            if (prefers != null && !prefers.isEmpty()) {
                target.setEndpoints(prefers);
            } else {
                LoadBalancer loadBalancer = getLoadBalancer(invocation);
                target.choose(endpoints -> {
                    List<? extends Endpoint> backends = endpoints;
                    do {
                        Candidate<? extends Endpoint> candidate = loadBalancer.elect(backends, invocation);
                        Endpoint backend = candidate == null ? null : candidate.getTarget();
                        if (backend == null) {
                            return null;
                        } else if (invocation.onElect(backend)) {
                            return Collections.singletonList(backend);
                        }
                        backends = backends == endpoints ? new ArrayList<>(endpoints) : backends;
                        backends.remove(candidate.getIndex());
                    } while (!backends.isEmpty());
                    return null;
                });
            }
        }
        if (logger.isDebugEnabled()) {
            logger.debug("LB filter applied to route target instance size: {}", target.size());
        }
        chain.filter(invocation);
    }

    /**
     * Attempts to prefer a sticky endpoint for the given route target and outbound invocation.
     *
     * @param target     the route target containing the list of endpoints.
     * @param invocation the outbound invocation to be forwarded.
     * @return a list containing the preferred sticky endpoint if forwarding is successful, or null if no sticky endpoint is found or forwarding fails.
     */
    private List<? extends Endpoint> preferSticky(RouteTarget target, OutboundInvocation<?> invocation) {
        // preferred sticky id
        String id = RequestContext.removeAttribute(Request.KEY_STICKY_ID);
        if (id != null && !id.isEmpty()) {
            Iterator<? extends Endpoint> iterator = target.getEndpoints().iterator();
            Endpoint endpoint;
            while (iterator.hasNext()) {
                endpoint = iterator.next();
                if (id.equals(endpoint.getId())) {
                    if (invocation.onElect(endpoint)) {
                        return Collections.singletonList(endpoint);
                    }
                    iterator.remove();
                    break;
                }
            }
        }
        return null;
    }

    /**
     * Retrieves the appropriate load balancer based on the service policy of the current invocation.
     *
     * @param invocation The current outbound invocation.
     * @return The load balancer to use for load balancing.
     */
    private LoadBalancer getLoadBalancer(OutboundInvocation<?> invocation) {
        ServicePolicy servicePolicy = invocation.getServiceMetadata().getServicePolicy();
        LoadBalancePolicy loadBalancePolicy = servicePolicy == null ? null : servicePolicy.getLoadBalancePolicy();
        String policyType = loadBalancePolicy == null ? null : loadBalancePolicy.getPolicyType();
        return invocation.getContext().getOrDefaultLoadBalancer(policyType);
    }

}
