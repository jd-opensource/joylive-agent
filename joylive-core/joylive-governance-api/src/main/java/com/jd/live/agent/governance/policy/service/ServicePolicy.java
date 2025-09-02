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
import com.jd.live.agent.governance.policy.PolicyIdGen;
import com.jd.live.agent.governance.policy.PolicyInherit.PolicyInheritWithIdGen;
import com.jd.live.agent.governance.policy.service.auth.AuthPolicy;
import com.jd.live.agent.governance.policy.service.auth.PermissionPolicy;
import com.jd.live.agent.governance.policy.service.circuitbreak.CircuitBreakPolicy;
import com.jd.live.agent.governance.policy.service.cluster.ClusterPolicy;
import com.jd.live.agent.governance.policy.service.fault.FaultInjectionPolicy;
import com.jd.live.agent.governance.policy.service.health.HealthPolicy;
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
    private HealthPolicy healthPolicy;

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
    private Boolean authorized;

    @Getter
    @Setter
    private AuthPolicy authPolicy;

    @Getter
    @Setter
    private List<AuthPolicy> authPolicies;

    @Getter
    @Setter
    private List<FaultInjectionPolicy> faultInjectionPolicies;

    private final transient Cache<String, LanePolicy> lanePolicyCache = new MapCache<>(new ListBuilder<>(() -> lanePolicies, LanePolicy::getLaneSpaceId));

    private final transient Cache<String, AuthPolicy> authPolicyCache = new MapCache<>(new ListBuilder<>(() -> authPolicies, AuthPolicy::getApplication));

    public ServicePolicy() {
    }

    @Override
    public void supplement(ServicePolicy source) {
        // ensure loadBalancePolicy is not null to hold stick id.
        if (loadBalancePolicy == null) {
            loadBalancePolicy = new LoadBalancePolicy();
        }
        supplementId(loadBalancePolicy);
        supplementId(clusterPolicy);
        supplementId(healthPolicy);
        supplementId(livePolicy);
        supplementId(authPolicy);
        supplementUri(rateLimitPolicies,
                new UriAppender<>(KEY_SERVICE_RATE_LIMIT, RateLimitPolicy::getName),
                new UriAppender<>(KEY_SERVICE_RATE_LIMIT_TYPE, RateLimitPolicy::getRealizeType));
        supplementUri(concurrencyLimitPolicies, new UriAppender<>(KEY_SERVICE_CONCURRENCY_LIMIT, ConcurrencyLimitPolicy::getName));
        supplementUri(loadLimitPolicies, new UriAppender<>(KEY_SERVICE_LOAD_LIMIT, LoadLimitPolicy::getName));
        supplementUri(routePolicies, new UriAppender<>(KEY_SERVICE_ROUTE, RoutePolicy::getName));
        supplementUri(lanePolicies, new UriAppender<>(KEY_SERVICE_LANE_SPACE_ID, LanePolicy::getLaneSpaceId));
        supplementUri(circuitBreakPolicies, new UriAppender<>(KEY_SERVICE_CIRCUIT_BREAK, CircuitBreakPolicy::getName));
        supplementUri(permissionPolicies, new UriAppender<>(KEY_SERVICE_AUTH, PermissionPolicy::getName));
        supplementUri(faultInjectionPolicies, new UriAppender<>(KEY_FAULT_INJECTION, FaultInjectionPolicy::getName));
        supplementUri(authPolicies, new UriAppender<>(KEY_SERVICE_CONSUMER, AuthPolicy::getApplication));

        if (source != null) {
            livePolicy = supplement(source.livePolicy, livePolicy, s -> new ServiceLivePolicy());
            clusterPolicy = supplement(source.clusterPolicy, clusterPolicy, s -> new ClusterPolicy());
            healthPolicy = supplement(source.healthPolicy, healthPolicy, s -> new HealthPolicy());
            loadBalancePolicy = supplement(source.loadBalancePolicy, loadBalancePolicy, s -> new LoadBalancePolicy());
            rateLimitPolicies = supplement(source.rateLimitPolicies, rateLimitPolicies, s -> new RateLimitPolicy(),
                    s -> uri.parameter(KEY_SERVICE_RATE_LIMIT, s.getName()));
            concurrencyLimitPolicies = supplement(source.concurrencyLimitPolicies, concurrencyLimitPolicies,
                    s -> new ConcurrencyLimitPolicy(),
                    s -> uri.parameter(KEY_SERVICE_CONCURRENCY_LIMIT, s.getName()));
            loadLimitPolicies = supplement(source.loadLimitPolicies, loadLimitPolicies, s -> new LoadLimitPolicy(),
                    s -> uri.parameter(KEY_SERVICE_LOAD_LIMIT, s.getName()));
            routePolicies = supplement(source.routePolicies, routePolicies, s -> new RoutePolicy(),
                    s -> uri.parameter(KEY_SERVICE_ROUTE, s.getName()));
            lanePolicies = supplement(source.lanePolicies, lanePolicies, s -> new LanePolicy(),
                    s -> uri.parameter(KEY_SERVICE_LANE_SPACE_ID, s.getLaneSpaceId()));
            circuitBreakPolicies = supplement(source.circuitBreakPolicies, circuitBreakPolicies,
                    s -> new CircuitBreakPolicy(),
                    s -> uri.parameter(KEY_SERVICE_CIRCUIT_BREAK, s.getName()));
            permissionPolicies = supplement(source.permissionPolicies, permissionPolicies, s -> new PermissionPolicy(),
                    s -> uri.parameter(KEY_SERVICE_AUTH, s.getName()));
            faultInjectionPolicies = supplement(source.faultInjectionPolicies, faultInjectionPolicies,
                    s -> new FaultInjectionPolicy(),
                    s -> uri.parameter(KEY_FAULT_INJECTION, s.getName()));
            authorized = authorized == null ? source.authorized : authorized;
            authPolicy = supplement(source.authPolicy, authPolicy, s -> new AuthPolicy());
            authPolicies = supplement(source.authPolicies, authPolicies, s -> new AuthPolicy(),
                    s -> uri.parameter(KEY_SERVICE_CONSUMER, s.getApplication()));
        }
    }

    public LanePolicy getLanePolicy(String laneSpaceId) {
        return lanePolicyCache.get(laneSpaceId);
    }

    public AuthPolicy getAuthPolicy(String application) {
        // for provider auth policy.
        AuthPolicy result = authPolicyCache.get(application);
        return result == null ? authPolicy : result;
    }

    public boolean authorized() {
        return authorized == null ? false : authorized;
    }

    @Override
    public ServicePolicy clone() {
        try {
            return (ServicePolicy) super.clone();
        } catch (CloneNotSupportedException e) {
            return null;
        }
    }

    protected void cache() {
        getLanePolicy("");
        getAuthPolicy("");
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
        if (loadLimitPolicies != null) {
            loadLimitPolicies.forEach(LoadLimitPolicy::cache);
        }
    }

    /**
     * Updates URIs for target policies by applying parameter transformations.
     *
     * @param targets Policies to modify (ignored if null/empty)
     * @param params  URI parameters to apply (name-value mappings per policy)
     */
    protected <V extends PolicyIdGen> void supplementUri(List<V> targets, UriAppender<V>... params) {
        if (targets != null && !targets.isEmpty() && params != null && params.length > 0) {
            targets.forEach(r -> r.supplement(() -> {
                URI uri = this.uri;
                for (UriAppender<V> p : params) {
                    uri = uri.parameter(p.name, p.valueFunc.apply(r));
                }
                return uri;
            }));
        }
    }

    /**
     * Creates or supplements policy instances:
     *
     * @return New list of supplemented policies (or original targets if no-op)
     */
    protected <T extends PolicyInheritWithIdGen<T>> List<T> supplement(List<T> sources,
                                                                       List<T> targets,
                                                                       Function<T, T> creator,
                                                                       Function<T, URI> uriFunc) {
        if (targets != null && !targets.isEmpty() || sources == null || sources.isEmpty()) {
            return targets;
        }
        List<T> result = new ArrayList<>(sources.size());
        for (T source : sources) {
            T newPolicy = creator.apply(source);
            newPolicy.supplement(() -> uriFunc.apply(source));
            newPolicy.supplement(source);
            result.add(newPolicy);
        }
        return result;
    }

    /**
     * URI parameter definition for policy-specific URI transformations.
     */
    @Getter
    protected static class UriAppender<V> {

        private final String name;

        private final Function<V, String> valueFunc;

        public UriAppender(String name, Function<V, String> valueFunc) {
            this.name = name;
            this.valueFunc = valueFunc;
        }
    }

}
