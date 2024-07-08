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
package com.jd.live.agent.governance.invoke.filter.outbound;

import com.jd.live.agent.core.extension.annotation.ConditionalOnProperty;
import com.jd.live.agent.core.extension.annotation.Extension;
import com.jd.live.agent.governance.config.GovernanceConfig;
import com.jd.live.agent.governance.context.RequestContext;
import com.jd.live.agent.governance.instance.Endpoint;
import com.jd.live.agent.governance.invoke.OutboundInvocation;
import com.jd.live.agent.governance.invoke.RouteTarget;
import com.jd.live.agent.governance.invoke.filter.OutboundFilter;
import com.jd.live.agent.governance.invoke.filter.OutboundFilterChain;
import com.jd.live.agent.governance.invoke.loadbalance.LoadBalancer;
import com.jd.live.agent.governance.policy.service.ServicePolicy;
import com.jd.live.agent.governance.policy.service.loadbalance.LoadBalancePolicy;
import com.jd.live.agent.governance.policy.service.loadbalance.StickyType;
import com.jd.live.agent.governance.request.Request;
import com.jd.live.agent.governance.request.ServiceRequest.OutboundRequest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * LoadBalanceFilter applies load balancing to the list of route targets. It ensures that
 * requests are distributed across available instances in a balanced manner based on the
 * configured load balancing policy.
 *
 * @since 1.0.0
 */
@Extension(value = "LoadBalanceFilter", order = OutboundFilter.ORDER_LOADBALANCE)
@ConditionalOnProperty(value = GovernanceConfig.CONFIG_FLOW_CONTROL_ENABLED, matchIfMissing = true)
public class LoadBalanceFilter implements OutboundFilter {

    @Override
    public <T extends OutboundRequest> void filter(OutboundInvocation<T> invocation, OutboundFilterChain chain) {
        RouteTarget target = invocation.getRouteTarget();
        ServicePolicy servicePolicy = invocation.getServiceMetadata().getServicePolicy();
        LoadBalancePolicy loadBalancePolicy = servicePolicy == null ? null : servicePolicy.getLoadBalancePolicy();
        StickyType stickyType = loadBalancePolicy == null ? null : loadBalancePolicy.getStickyType();
        stickyType = stickyType == null ? StickyType.NONE : stickyType;
        if (!target.isEmpty()) {
            List<? extends Endpoint> candidates = stickyType == StickyType.PREFERRED ? preferSticky(target, invocation) : null;
            if (candidates != null && !candidates.isEmpty()) {
                target.setEndpoints(candidates);
            } else {
                LoadBalancer loadBalancer = getLoadBalancer(invocation);
                target.choose(endpoints -> {
                    List<? extends Endpoint> backends = endpoints;
                    do {
                        Endpoint backend = loadBalancer.choose(backends, invocation);
                        if (backend == null) {
                            return null;
                        } else if (invocation.onForward(backend)) {
                            return Collections.singletonList(backend);
                        } else {
                            invocation.onCancel(backend);
                        }
                        backends = backends == endpoints ? new ArrayList<>(endpoints) : backends;
                        backends.remove(backend);
                    } while (!backends.isEmpty());
                    return null;
                });
            }
            if (stickyType != StickyType.NONE) {
                candidates = target.getEndpoints();
                if (candidates != null && !candidates.isEmpty()) {
                    invocation.getRequest().setStickyId(candidates.get(0).getId());
                }
            }
        }

        T request = invocation.getRequest();
        if (!target.isEmpty()) {
            Endpoint endpoint = target.getEndpoints().get(0);
            request.addAttempt(endpoint.getId());
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
        if (id != null) {
            List<? extends Endpoint> backends = RouteTarget.tryCopy(target.getEndpoints(),
                    endpoint -> id.equals(endpoint.getId()), 1);
            if (!backends.isEmpty()) {
                Endpoint endpoint = backends.get(0);
                if (invocation.onForward(endpoint)) {
                    return backends;
                }
                invocation.onCancel(endpoint);
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
