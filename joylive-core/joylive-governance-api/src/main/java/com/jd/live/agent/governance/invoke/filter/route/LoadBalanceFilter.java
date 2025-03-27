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
import com.jd.live.agent.governance.context.bag.Carrier;
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

    @Override
    public <T extends OutboundRequest> void filter(OutboundInvocation<T> invocation, RouteFilterChain chain) {
        RouteTarget target = invocation.getRouteTarget();
        int size = target.size();
        if (size > 0) {
            List<? extends Endpoint> prefers = preferSticky(target, invocation);
            if (prefers != null && !prefers.isEmpty()) {
                target.setEndpoints(prefers);
            } else {
                ServicePolicy servicePolicy = invocation.getServiceMetadata().getServicePolicy();
                LoadBalancePolicy loadBalancePolicy = servicePolicy == null ? null : servicePolicy.getLoadBalancePolicy();
                String policyType = loadBalancePolicy == null ? null : loadBalancePolicy.getPolicyType();
                LoadBalancer loadBalancer = invocation.getContext().getOrDefaultLoadBalancer(policyType);
                target.choose(endpoints -> {
                    List<? extends Endpoint> backends = endpoints;
                    do {
                        Candidate<? extends Endpoint> candidate = loadBalancer.elect(backends, loadBalancePolicy, invocation);
                        Endpoint backend = candidate == null ? null : candidate.getTarget();
                        if (backend == null) {
                            return null;
                        } else if (invocation.onElect(backend)) {
                            if (candidate.getCounter() != null) {
                                invocation.getRequest().setAttribute(Endpoint.ATTRIBUTE_COUNTER, candidate.getCounter());
                            }
                            return Collections.singletonList(backend);
                        }
                        backends = backends == endpoints ? new ArrayList<>(endpoints) : backends;
                        backends.remove(candidate.getIndex());
                    } while (!backends.isEmpty());
                    return null;
                });
            }
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
        Carrier carrier = invocation.getRequest().getCarrier();
        String id = carrier == null ? null : carrier.removeAttribute(Request.KEY_STICKY_ID);
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

}
