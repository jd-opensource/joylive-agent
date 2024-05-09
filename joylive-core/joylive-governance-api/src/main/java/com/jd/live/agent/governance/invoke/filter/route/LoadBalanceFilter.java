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

import com.jd.live.agent.bootstrap.classloader.ResourcerType;
import com.jd.live.agent.core.extension.annotation.ConditionalOnProperty;
import com.jd.live.agent.core.extension.annotation.Extension;
import com.jd.live.agent.core.inject.annotation.Inject;
import com.jd.live.agent.core.inject.annotation.InjectLoader;
import com.jd.live.agent.core.inject.annotation.Injectable;
import com.jd.live.agent.governance.config.GovernanceConfig;
import com.jd.live.agent.governance.instance.Endpoint;
import com.jd.live.agent.governance.invoke.OutboundInvocation;
import com.jd.live.agent.governance.invoke.RouteTarget;
import com.jd.live.agent.governance.invoke.filter.RouteFilter;
import com.jd.live.agent.governance.invoke.filter.RouteFilterChain;
import com.jd.live.agent.governance.invoke.loadbalance.LoadBalancer;
import com.jd.live.agent.governance.invoke.loadbalance.randomweight.RandomWeightLoadBalancer;
import com.jd.live.agent.governance.policy.service.ServicePolicy;
import com.jd.live.agent.governance.policy.service.loadbalance.LoadBalancePolicy;
import com.jd.live.agent.governance.request.ServiceRequest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/**
 * LoadBalanceFilter applies load balancing to the list of route targets. It ensures that
 * requests are distributed across available instances in a balanced manner based on the
 * configured load balancing policy.
 */
@Injectable
@Extension(value = "LoadBalanceFilter", order = RouteFilter.ORDER_LOADBALANCE)
@ConditionalOnProperty(value = GovernanceConfig.CONFIG_FLOW_CONTROL_ENABLED, matchIfMissing = true)
@ConditionalOnProperty(value = GovernanceConfig.CONFIG_LOADBALANCE_ENABLED, matchIfMissing = true)
public class LoadBalanceFilter implements RouteFilter {

    @Inject
    @InjectLoader(ResourcerType.CORE_IMPL)
    private Map<String, LoadBalancer> loadBalancers;

    @Override
    public <T extends ServiceRequest.OutboundRequest> void filter(OutboundInvocation<T> invocation, RouteFilterChain chain) {
        RouteTarget target = invocation.getRouteTarget();
        if (!target.isEmpty()) {
            LoadBalancer loadBalancer = getLoadBalancer(invocation);
            target.choose(endpoints -> {
                List<? extends Endpoint> backends = endpoints;
                do {
                    Endpoint backend = loadBalancer.choose(backends, invocation);
                    if (backend == null) {
                        return null;
                    }
                    Predicate<Endpoint> predicate = backend.getPredicate();
                    if (predicate == null || predicate.test(backend)) {
                        return Collections.singletonList(backend);
                    }
                    backends = backends == endpoints ? new ArrayList<>(endpoints) : backends;
                    backends.remove(backend);
                } while (!endpoints.isEmpty());
                return null;
            });
        }

        T request = invocation.getRequest();
        if (target.isEmpty()) {
            RuntimeException exception = request.createNoAvailableEndpointException();
            if (exception != null) {
                throw exception;
            }
        } else {
            Endpoint endpoint = target.getEndpoints().get(0);
            request.addAttempt(endpoint.getId());
        }

        chain.filter(invocation);
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
        LoadBalancer loadBalancer = loadBalancePolicy == null ? null : loadBalancers.get(loadBalancePolicy.getPolicyType());
        // If no load balancer is found, use a default random-weight load balancer
        loadBalancer = loadBalancer == null ? RandomWeightLoadBalancer.INSTANCE : loadBalancer;
        return loadBalancer;
    }

}
