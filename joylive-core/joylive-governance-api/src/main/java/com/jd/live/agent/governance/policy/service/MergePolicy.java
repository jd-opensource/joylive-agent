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

/**
 * Enumeration representing different synchronization policies for service policies.
 */
public enum MergePolicy implements PolicyMerger {

    /**
     * Represents a synchronization policy that includes all service policies.
     */
    ALL {
        @Override
        public void onAdd(ServicePolicy newPolicy) {
        }

        @Override
        public void onDelete(ServicePolicy oldPolicy) {
            if (oldPolicy != null) {
                oldPolicy.setClusterPolicy(null);
                oldPolicy.setLanePolicies(null);
                oldPolicy.setRoutePolicies(null);
                oldPolicy.setLoadBalancePolicy(null);
                oldPolicy.setConcurrencyLimitPolicies(null);
                oldPolicy.setRateLimitPolicies(null);
                oldPolicy.setCircuitBreakerPolicies(null);
                oldPolicy.setLivePolicy(null);
            }
        }

        @Override
        public void onUpdate(ServicePolicy oldPolicy, ServicePolicy newPolicy) {
            oldPolicy.setClusterPolicy(newPolicy.getClusterPolicy());
            oldPolicy.setLanePolicies(newPolicy.getLanePolicies());
            oldPolicy.setRoutePolicies(newPolicy.getRoutePolicies());
            oldPolicy.setLoadBalancePolicy(newPolicy.getLoadBalancePolicy());
            oldPolicy.setConcurrencyLimitPolicies(newPolicy.getConcurrencyLimitPolicies());
            oldPolicy.setRateLimitPolicies(newPolicy.getRateLimitPolicies());
            oldPolicy.setCircuitBreakerPolicies(newPolicy.getCircuitBreakerPolicies());
            oldPolicy.setLivePolicy(newPolicy.getLivePolicy());
        }
    },

    /**
     * Represents a synchronization policy that includes only flow control policies,
     * excluding the live service policy.
     */
    FLOW_CONTROL {
        @Override
        public void onAdd(ServicePolicy newPolicy) {
            if (newPolicy != null) {
                newPolicy.setLivePolicy(null);
            }
        }

        @Override
        public void onDelete(ServicePolicy oldPolicy) {
            if (oldPolicy != null) {
                oldPolicy.setClusterPolicy(null);
                oldPolicy.setLanePolicies(null);
                oldPolicy.setRoutePolicies(null);
                oldPolicy.setLoadBalancePolicy(null);
                oldPolicy.setConcurrencyLimitPolicies(null);
                oldPolicy.setRateLimitPolicies(null);
                oldPolicy.setCircuitBreakerPolicies(null);
            }
        }

        @Override
        public void onUpdate(ServicePolicy oldPolicy, ServicePolicy newPolicy) {
            if (oldPolicy != null && newPolicy != null) {
                oldPolicy.setClusterPolicy(newPolicy.getClusterPolicy());
                oldPolicy.setLanePolicies(newPolicy.getLanePolicies());
                oldPolicy.setRoutePolicies(newPolicy.getRoutePolicies());
                oldPolicy.setLoadBalancePolicy(newPolicy.getLoadBalancePolicy());
                oldPolicy.setConcurrencyLimitPolicies(newPolicy.getConcurrencyLimitPolicies());
                oldPolicy.setRateLimitPolicies(newPolicy.getRateLimitPolicies());
                oldPolicy.setCircuitBreakerPolicies(newPolicy.getCircuitBreakerPolicies());
            }
        }
    },

    /**
     * Represents a synchronization policy that includes only service live policies.
     */
    LIVE {
        @Override
        public void onAdd(ServicePolicy newPolicy) {
            newPolicy.setClusterPolicy(null);
            newPolicy.setLanePolicies(null);
            newPolicy.setRoutePolicies(null);
            newPolicy.setLoadBalancePolicy(null);
            newPolicy.setConcurrencyLimitPolicies(null);
            newPolicy.setRateLimitPolicies(null);
            newPolicy.setCircuitBreakerPolicies(null);
        }

        @Override
        public void onDelete(ServicePolicy oldPolicy) {
            oldPolicy.setLivePolicy(null);
        }

        @Override
        public void onUpdate(ServicePolicy oldPolicy, ServicePolicy newPolicy) {
            oldPolicy.setLivePolicy(newPolicy.getLivePolicy());
        }
    };
}
