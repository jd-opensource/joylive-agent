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
package com.jd.live.agent.governance.policy.service;

import com.jd.live.agent.core.util.URI;
import com.jd.live.agent.core.util.cache.Cache;
import com.jd.live.agent.core.util.cache.MapCache;
import com.jd.live.agent.core.util.map.ListBuilder;
import com.jd.live.agent.governance.policy.PolicyId;
import com.jd.live.agent.governance.policy.PolicyInherit;
import com.jd.live.agent.governance.policy.PolicyInherit.PolicyInheritWithIdGen;
import com.jd.live.agent.governance.policy.service.auth.AuthPolicy;
import com.jd.live.agent.governance.policy.service.auth.PermissionPolicy;
import com.jd.live.agent.governance.policy.service.circuitbreak.CircuitBreakPolicy;
import com.jd.live.agent.governance.policy.service.cluster.ClusterPolicy;
import com.jd.live.agent.governance.policy.service.fault.FaultInjectionPolicy;
import com.jd.live.agent.governance.policy.service.lane.LanePolicy;
import com.jd.live.agent.governance.policy.service.limit.ConcurrencyLimitPolicy;
import com.jd.live.agent.governance.policy.service.limit.LoadLimitPolicy;
import com.jd.live.agent.governance.policy.service.limit.RateLimitPolicy;
import com.jd.live.agent.governance.policy.service.live.ServiceLivePolicy;
import com.jd.live.agent.governance.policy.service.loadbalance.LoadBalancePolicy;
import com.jd.live.agent.governance.policy.service.route.RoutePolicy;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * ServicePolicy
 */
public class ServicePolicy extends PolicyId implements Cloneable, PolicyInheritWithIdGen<ServicePolicy> {

    @Setter
    @Getter
    private LoadBalancePolicy loadBalancePolicy;

    @Setter
    @Getter
    private ClusterPolicy clusterPolicy;

    @Setter
    @Getter
    private List<RateLimitPolicy> rateLimitPolicies;

    @Setter
    @Getter
    private List<ConcurrencyLimitPolicy> concurrencyLimitPolicies;

    @Setter
    @Getter
    private List<LoadLimitPolicy> loadLimitPolicies;

    @Setter
    @Getter
    private List<RoutePolicy> routePolicies;

    @Setter
    @Getter
    private ServiceLivePolicy livePolicy;

    @Setter
    @Getter
    private List<LanePolicy> lanePolicies;

    @Setter
    @Getter
    private List<CircuitBreakPolicy> circuitBreakPolicies;

    @Setter
    @Getter
    private List<PermissionPolicy> permissionPolicies;

    @Getter
    @Setter
    private AuthPolicy authPolicy;

    @Getter
    @Setter
    private List<FaultInjectionPolicy> faultInjectionPolicies;

    private final transient Cache<String, LanePolicy> lanePolicyCache = new MapCache<>(new ListBuilder<>(() -> lanePolicies, LanePolicy::getLaneSpaceId));

    public ServicePolicy() {
    }

    @Override
    public void supplement(ServicePolicy source) {
        if (loadBalancePolicy != null && loadBalancePolicy.getId() == null) {
            loadBalancePolicy.setId(id);
        }
        if (clusterPolicy != null && clusterPolicy.getId() == null) {
            clusterPolicy.setId(id);
        }
        if (livePolicy != null && livePolicy.getId() == null) {
            livePolicy.setId(id);
        }
        if (rateLimitPolicies != null && !rateLimitPolicies.isEmpty()) {
            rateLimitPolicies.forEach(r -> r.supplement(() -> uri.parameter(KEY_SERVICE_RATE_LIMIT, r.getName())));
        }
        if (concurrencyLimitPolicies != null && !concurrencyLimitPolicies.isEmpty()) {
            concurrencyLimitPolicies.forEach(r -> r.supplement(() -> uri.parameter(KEY_SERVICE_CONCURRENCY_LIMIT, r.getName())));
        }
        if (loadLimitPolicies != null && !loadLimitPolicies.isEmpty()) {
            loadLimitPolicies.forEach(r -> r.supplement(() -> uri.parameter(KEY_SERVICE_LOAD_LIMIT, r.getName())));
        }
        if (routePolicies != null && !routePolicies.isEmpty()) {
            routePolicies.forEach(r -> r.supplement(() -> uri.parameter(KEY_SERVICE_ROUTE, r.getName())));
        }
        if (lanePolicies != null && !lanePolicies.isEmpty()) {
            lanePolicies.forEach(r -> r.supplement(() -> uri.parameter(KEY_SERVICE_LANE_SPACE_ID, r.getLaneSpaceId())));
        }
        if (circuitBreakPolicies != null && !circuitBreakPolicies.isEmpty()) {
            circuitBreakPolicies.forEach(r -> r.supplement(() -> uri.parameter(KEY_SERVICE_CIRCUIT_BREAK, r.getName())));
        }
        if (permissionPolicies != null && !permissionPolicies.isEmpty()) {
            permissionPolicies.forEach(r -> r.supplement(() -> uri.parameter(KEY_SERVICE_AUTH, r.getName())));
        }
        if (faultInjectionPolicies != null && !faultInjectionPolicies.isEmpty()) {
            faultInjectionPolicies.forEach(r -> r.supplement(() -> uri.parameter(KEY_FAULT_INJECTION, r.getName())));
        }
        if (authPolicy != null && authPolicy.getId() != null) {
            authPolicy.setId(id);
        }

        if (source != null) {
            livePolicy = copy(source.livePolicy, livePolicy, s -> new ServiceLivePolicy());
            clusterPolicy = copy(source.clusterPolicy, clusterPolicy, s -> new ClusterPolicy());
            loadBalancePolicy = copy(source.loadBalancePolicy, loadBalancePolicy, s -> new LoadBalancePolicy());
            authPolicy = copy(source.authPolicy, authPolicy, s -> new AuthPolicy());

            if ((rateLimitPolicies == null || rateLimitPolicies.isEmpty()) &&
                    (source.rateLimitPolicies != null && !source.rateLimitPolicies.isEmpty())) {
                rateLimitPolicies = copy(source.rateLimitPolicies,
                        s -> new RateLimitPolicy(),
                        s -> uri.parameter(KEY_SERVICE_RATE_LIMIT, s.getName()));
            }
            if ((concurrencyLimitPolicies == null || concurrencyLimitPolicies.isEmpty()) &&
                    (source.concurrencyLimitPolicies != null && !source.concurrencyLimitPolicies.isEmpty())) {
                concurrencyLimitPolicies = copy(source.concurrencyLimitPolicies,
                        s -> new ConcurrencyLimitPolicy(),
                        s -> uri.parameter(KEY_SERVICE_CONCURRENCY_LIMIT, s.getName()));
            }
            if ((loadLimitPolicies == null || loadLimitPolicies.isEmpty()) &&
                    (source.loadLimitPolicies != null && !source.loadLimitPolicies.isEmpty())) {
                loadLimitPolicies = copy(source.loadLimitPolicies,
                        s -> new LoadLimitPolicy(),
                        s -> uri.parameter(KEY_SERVICE_LOAD_LIMIT, s.getName()));
            }
            if ((routePolicies == null || routePolicies.isEmpty()) &&
                    (source.routePolicies != null && !source.routePolicies.isEmpty())) {
                routePolicies = copy(source.routePolicies,
                        s -> new RoutePolicy(),
                        s -> uri.parameter(KEY_SERVICE_ROUTE, s.getName()));
            }
            if ((lanePolicies == null || lanePolicies.isEmpty()) &&
                    (source.lanePolicies != null && !source.lanePolicies.isEmpty())) {
                lanePolicies = copy(source.lanePolicies,
                        s -> new LanePolicy(),
                        s -> uri.parameter(KEY_SERVICE_LANE_SPACE_ID, s.getLaneSpaceId()));
            }
            if ((circuitBreakPolicies == null || circuitBreakPolicies.isEmpty()) &&
                    (source.circuitBreakPolicies != null && !source.circuitBreakPolicies.isEmpty())) {
                circuitBreakPolicies = copy(source.circuitBreakPolicies,
                        s -> new CircuitBreakPolicy(),
                        s -> uri.parameter(KEY_SERVICE_CIRCUIT_BREAK, s.getName()));
            }
            if ((permissionPolicies == null || permissionPolicies.isEmpty()) &&
                    (source.permissionPolicies != null && !source.permissionPolicies.isEmpty())) {
                permissionPolicies = copy(source.permissionPolicies,
                        s -> new PermissionPolicy(),
                        s -> uri.parameter(KEY_SERVICE_AUTH, s.getName()));
            }
            if ((faultInjectionPolicies == null || faultInjectionPolicies.isEmpty()) &&
                    (source.faultInjectionPolicies != null && !source.faultInjectionPolicies.isEmpty())) {
                faultInjectionPolicies = copy(source.faultInjectionPolicies,
                        s -> new FaultInjectionPolicy(),
                        s -> uri.parameter(KEY_FAULT_INJECTION, s.getName()));
            }
        }
    }

    protected <T extends PolicyInherit.PolicyInheritWithId<T>> T copy(T source,
                                                                      T target,
                                                                      Function<T, T> creator) {
        if (source != null) {
            if (target == null) {
                target = creator.apply(source);
                target.setId(id);
            }
            target.supplement(source);
        }
        return target;
    }

    protected <T extends PolicyInheritWithIdGen<T>> List<T> copy(List<T> sources,
                                                                 Function<T, T> creator,
                                                                 Function<T, URI> uriFunc) {
        List<T> result = new ArrayList<>(sources.size());
        for (T source : sources) {
            T newPolicy = creator.apply(source);
            newPolicy.supplement(() -> uriFunc.apply(source));
            newPolicy.supplement(source);
            result.add(newPolicy);
        }
        return result;
    }

    public LanePolicy getLanePolicy(String laneSpaceId) {
        return lanePolicyCache.get(laneSpaceId);
    }

    protected void cache() {
        getLanePolicy("");
        if (livePolicy != null) {
            livePolicy.cache();
        }
        if (routePolicies != null) {
            routePolicies.forEach(RoutePolicy::cache);
        }
        if (clusterPolicy != null) {
            clusterPolicy.cache();
        }
        if (circuitBreakPolicies != null) {
            circuitBreakPolicies.forEach(CircuitBreakPolicy::cache);
        }
    }

    @Override
    public ServicePolicy clone() {
        try {
            return (ServicePolicy) super.clone();
        } catch (CloneNotSupportedException e) {
            return null;
        }
    }

}
