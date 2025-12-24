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

import com.jd.live.agent.core.extension.annotation.ConditionalComparison;
import com.jd.live.agent.core.extension.annotation.ConditionalOnProperty;
import com.jd.live.agent.core.extension.annotation.Extension;
import com.jd.live.agent.core.instance.Location;
import com.jd.live.agent.governance.annotation.ConditionalOnFlowControlEnabled;
import com.jd.live.agent.governance.config.GovernanceConfig;
import com.jd.live.agent.governance.config.ServiceConfig;
import com.jd.live.agent.governance.instance.Endpoint;
import com.jd.live.agent.governance.invoke.OutboundInvocation;
import com.jd.live.agent.governance.invoke.RouteTarget;
import com.jd.live.agent.governance.invoke.filter.LiveFilter;
import com.jd.live.agent.governance.invoke.filter.RouteFilter;
import com.jd.live.agent.governance.invoke.filter.RouteFilterChain;
import com.jd.live.agent.governance.invoke.metadata.ServiceMetadata;
import com.jd.live.agent.governance.policy.service.live.CellPolicy;
import com.jd.live.agent.governance.policy.service.live.ServiceLivePolicy;
import com.jd.live.agent.governance.request.ServiceRequest.OutboundRequest;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

import static com.jd.live.agent.core.util.StringUtils.isEmpty;

/**
 * Local first filter
 */
@Extension(value = "LocalFilter", order = RouteFilter.ORDER_LOCAL)
@ConditionalOnFlowControlEnabled
@ConditionalOnProperty(name = GovernanceConfig.CONFIG_LIVE_ENABLED, value = "true", matchIfMissing = true, comparison = ConditionalComparison.NOT_EQUAL)
public class LocalFilter implements RouteFilter, LiveFilter {

    @Override
    public <T extends OutboundRequest> void filter(final OutboundInvocation<T> invocation, final RouteFilterChain chain) {
        Location location = invocation.getContext().getLocation();
        String region = location.getRegion();
        String zone = location.getZone();
        String cluster = location.getCluster();
        Predicate<Endpoint> regionPredicate = isEmpty(region) ? null : endpoint -> endpoint.isRegion(region);
        Predicate<Endpoint> zonePredicate = isEmpty(zone) ? null : endpoint -> endpoint.isZone(zone);
        Predicate<Endpoint> clusterPredicate = isEmpty(cluster) ? null : endpoint -> endpoint.isCluster(cluster);

        // with location
        if (regionPredicate != null || zonePredicate != null || clusterPredicate != null) {
            ServiceMetadata metadata = invocation.getServiceMetadata();
            ServiceConfig serviceConfig = metadata.getServiceConfig();
            ServiceLivePolicy livePolicy = metadata.getServiceLivePolicy();
            CellPolicy cellPolicy = livePolicy == null ? null : livePolicy.getCellPolicy();
            // local first
            boolean localFirst = cellPolicy == CellPolicy.PREFER_LOCAL_CELL || cellPolicy == null && serviceConfig.isLocalFirst();
            if (localFirst) {
                Function<String, Integer> thresholdFunc = cellPolicy == CellPolicy.PREFER_LOCAL_CELL
                        ? livePolicy::getCellThreshold
                        : serviceConfig::getCellFailoverThreshold;
                route(invocation, regionPredicate, zonePredicate, clusterPredicate, thresholdFunc.apply(zone));
            }
        }
        chain.filter(invocation);
    }

    /**
     * Routes endpoints based on locality preferences with threshold-based failover.
     * Prioritizes cluster-level endpoints, then zone, then region for optimal latency.
     *
     * @param invocation       the outbound invocation containing routing context
     * @param regionPredicate  filters endpoints by region locality
     * @param zonePredicate    filters endpoints by zone locality
     * @param clusterPredicate filters endpoints by cluster locality
     * @param threshold        minimum endpoint count required for valid routing group
     */
    private void route(OutboundInvocation<?> invocation,
                       Predicate<Endpoint> regionPredicate,
                       Predicate<Endpoint> zonePredicate,
                       Predicate<Endpoint> clusterPredicate,
                       Integer threshold) {
        threshold = threshold == null || threshold <= 0 ? 1 : threshold;
        RouteTarget target = invocation.getRouteTarget();
        int size = target.size();
        if (size == 0 || size <= threshold) {
            return;
        }
        List<Endpoint> localClusterEndpoints = new ArrayList<>(size / 2);
        List<Endpoint> localZoneEndpoints = new ArrayList<>(size / 2);
        List<Endpoint> localRegionEndpoints = new ArrayList<>(size / 2);
        List<Endpoint> otherEndpoints = new ArrayList<>(size / 2);
        for (Endpoint endpoint : target.getEndpoints()) {
            if (regionPredicate != null && regionPredicate.test(endpoint)) {
                if (zonePredicate != null && zonePredicate.test(endpoint)) {
                    if (clusterPredicate != null && clusterPredicate.test(endpoint)) {
                        localClusterEndpoints.add(endpoint);
                    } else {
                        localZoneEndpoints.add(endpoint);
                    }
                } else {
                    localRegionEndpoints.add(endpoint);
                }
            } else if (regionPredicate == null && zonePredicate != null && zonePredicate.test(endpoint)) {
                if (clusterPredicate != null && clusterPredicate.test(endpoint)) {
                    localClusterEndpoints.add(endpoint);
                } else {
                    localZoneEndpoints.add(endpoint);
                }
            } else if (regionPredicate == null && zonePredicate == null && clusterPredicate != null && clusterPredicate.test(endpoint)) {
                localClusterEndpoints.add(endpoint);
            } else {
                otherEndpoints.add(endpoint);
            }
        }

        List<Endpoint>[] candidates = new List[]{localClusterEndpoints, localZoneEndpoints, localRegionEndpoints, otherEndpoints};
        int random = invocation.getRandom().nextInt(threshold);
        int count = 0;
        for (List<Endpoint> candidate : candidates) {
            count += candidate.size();
            if (count >= random) {
                target.setEndpoints(candidate);
                break;
            }
        }
    }
}
