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
import com.jd.live.agent.governance.policy.service.auth.PermissionPolicy;
import com.jd.live.agent.governance.policy.service.circuitbreak.CircuitBreakPolicy;
import com.jd.live.agent.governance.policy.service.cluster.ClusterPolicy;
import com.jd.live.agent.governance.policy.service.cluster.RetryPolicy;
import com.jd.live.agent.governance.policy.service.fault.FaultInjectionPolicy;
import com.jd.live.agent.governance.policy.service.lane.LanePolicy;
import com.jd.live.agent.governance.policy.service.limit.ConcurrencyLimitPolicy;
import com.jd.live.agent.governance.policy.service.limit.LoadLimitPolicy;
import com.jd.live.agent.governance.policy.service.limit.RateLimitPolicy;
import com.jd.live.agent.governance.policy.service.live.ServiceLivePolicy;
import com.jd.live.agent.governance.policy.service.loadbalance.LoadBalancePolicy;
import com.jd.live.agent.governance.policy.service.route.RoutePolicy;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static com.jd.live.agent.governance.policy.PolicyId.*;

public class ServicePolicyTest {

    @Test
    void testSupplement() {
        ServicePolicy source = null;
        ServicePolicy target = new ServicePolicy();
        target.setId(123L);
        target.supplement(() -> URI.builder().scheme("service").host("order").path("/").build().parameter("group", "test"));
        target.setLoadBalancePolicy(new LoadBalancePolicy());
        target.setClusterPolicy(new ClusterPolicy());
        target.setLivePolicy(new ServiceLivePolicy());
        target.setRateLimitPolicies(Arrays.asList(new RateLimitPolicy("rateLimitPolicy1", "LeakyBucket")));
        target.setConcurrencyLimitPolicies(Arrays.asList(new ConcurrencyLimitPolicy("concurrencyLimitPolicy1")));
        target.setLoadLimitPolicies(Arrays.asList(new LoadLimitPolicy("loadLimitPolicy1")));
        target.setRoutePolicies(Arrays.asList(new RoutePolicy("routePolicy1")));
        target.setLanePolicies(Arrays.asList(new LanePolicy("laneSpaceId1")));
        target.setCircuitBreakPolicies(Arrays.asList(new CircuitBreakPolicy("circuitBreakPolicy1")));
        target.setPermissionPolicies(Arrays.asList(new PermissionPolicy("permissionPolicy1")));
        target.setFaultInjectionPolicies(Arrays.asList(new FaultInjectionPolicy("faultInjectionPolicy1")));
        target.supplement(source);


        Assertions.assertEquals(target.getId(), target.getLoadBalancePolicy().getId());
        Assertions.assertEquals(target.getId(), target.getClusterPolicy().getId());
        Assertions.assertEquals(target.getId(), target.getLivePolicy().getId());
        Assertions.assertEquals("rateLimitPolicy1", target.getRateLimitPolicies().get(0).getUri().getParameter(KEY_SERVICE_RATE_LIMIT));
        Assertions.assertEquals("concurrencyLimitPolicy1", target.getConcurrencyLimitPolicies().get(0).getUri().getParameter(KEY_SERVICE_CONCURRENCY_LIMIT));
        Assertions.assertEquals("loadLimitPolicy1", target.getLoadLimitPolicies().get(0).getUri().getParameter(KEY_SERVICE_LOAD_LIMIT));
        Assertions.assertEquals("routePolicy1", target.getRoutePolicies().get(0).getUri().getParameter(KEY_SERVICE_ROUTE));
        Assertions.assertEquals("laneSpaceId1", target.getLanePolicies().get(0).getUri().getParameter(KEY_SERVICE_LANE_SPACE_ID));
        Assertions.assertEquals("circuitBreakPolicy1", target.getCircuitBreakPolicies().get(0).getUri().getParameter(KEY_SERVICE_CIRCUIT_BREAK));
        Assertions.assertEquals("permissionPolicy1", target.getPermissionPolicies().get(0).getUri().getParameter(KEY_SERVICE_AUTH));
        Assertions.assertEquals("faultInjectionPolicy1", target.getFaultInjectionPolicies().get(0).getUri().getParameter(KEY_FAULT_INJECTION));

        source = target;
        target = new ServicePolicy();
        target.setId(456L);
        target.supplement(() -> URI.builder().scheme("service").host("order").path("/add").build().parameter("group", "test"));
        target.supplement(source);
        Assertions.assertNotNull(target.getLoadBalancePolicy());
        Assertions.assertNotNull(target.getClusterPolicy());
        Assertions.assertNotNull(target.getLivePolicy());
        Assertions.assertEquals(1, target.getRateLimitPolicies().size());
        Assertions.assertEquals("rateLimitPolicy1", target.getRateLimitPolicies().get(0).getUri().getParameter(KEY_SERVICE_RATE_LIMIT));
        Assertions.assertEquals(1, target.getConcurrencyLimitPolicies().size());
        Assertions.assertEquals("concurrencyLimitPolicy1", target.getConcurrencyLimitPolicies().get(0).getUri().getParameter(KEY_SERVICE_CONCURRENCY_LIMIT));
        Assertions.assertEquals(1, target.getLoadLimitPolicies().size());
        Assertions.assertEquals("loadLimitPolicy1", target.getLoadLimitPolicies().get(0).getUri().getParameter(KEY_SERVICE_LOAD_LIMIT));
        Assertions.assertEquals(1, target.getRoutePolicies().size());
        Assertions.assertEquals("routePolicy1", target.getRoutePolicies().get(0).getUri().getParameter(KEY_SERVICE_ROUTE));
        Assertions.assertEquals(1, target.getLanePolicies().size());
        Assertions.assertEquals("laneSpaceId1", target.getLanePolicies().get(0).getUri().getParameter(KEY_SERVICE_LANE_SPACE_ID));
        Assertions.assertEquals(1, target.getCircuitBreakPolicies().size());
        Assertions.assertEquals("circuitBreakPolicy1", target.getCircuitBreakPolicies().get(0).getUri().getParameter(KEY_SERVICE_CIRCUIT_BREAK));
        Assertions.assertEquals(1, target.getPermissionPolicies().size());
        Assertions.assertEquals("permissionPolicy1", target.getPermissionPolicies().get(0).getUri().getParameter(KEY_SERVICE_AUTH));
        Assertions.assertEquals(1, target.getFaultInjectionPolicies().size());
        Assertions.assertEquals("faultInjectionPolicy1", target.getFaultInjectionPolicies().get(0).getUri().getParameter(KEY_FAULT_INJECTION));

        ClusterPolicy clusterPolicy = new ClusterPolicy();
        clusterPolicy.setId(123L);
        RetryPolicy retryPolicy = new RetryPolicy();
        retryPolicy.setRetry(2);
        clusterPolicy.setRetryPolicy(retryPolicy);
        ClusterPolicy targetClusterPolicy = new ClusterPolicy();
        targetClusterPolicy.setId(222L);
        targetClusterPolicy.supplement(clusterPolicy);
        Assertions.assertNotNull(targetClusterPolicy.getRetryPolicy());
        Assertions.assertEquals(2, targetClusterPolicy.getRetryPolicy().getRetry());

    }

}
