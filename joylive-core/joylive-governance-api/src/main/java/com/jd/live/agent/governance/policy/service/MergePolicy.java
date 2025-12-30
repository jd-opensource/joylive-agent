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
            FLOW_CONTROL.onDelete(oldPolicy);
            LIVE.onDelete(oldPolicy);
        }

        @Override
        public void onUpdate(ServicePolicy oldPolicy, ServicePolicy newPolicy) {
            FLOW_CONTROL.onUpdate(oldPolicy, newPolicy);
            LIVE.onUpdate(oldPolicy, newPolicy);
        }

        @Override
        public void onDelete(Service service) {
            FLOW_CONTROL.onDelete(service);
        }

        @Override
        public void onUpdate(Service oldService, Service newService) {
            FLOW_CONTROL.onUpdate(oldService, newService);
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
                oldPolicy.setLoadBalancePolicy(null);
                oldPolicy.setClusterPolicy(null);
                oldPolicy.setHealthPolicy(null);
                oldPolicy.setRateLimitPolicies(null);
                oldPolicy.setConcurrencyLimitPolicies(null);
                oldPolicy.setLoadLimitPolicies(null);
                oldPolicy.setRoutePolicies(null);
                oldPolicy.setLanePolicies(null);
                oldPolicy.setCircuitBreakPolicies(null);
                oldPolicy.setPermissionPolicies(null);
                oldPolicy.setFaultInjectionPolicies(null);
            }
        }

        @Override
        public void onUpdate(ServicePolicy oldPolicy, ServicePolicy newPolicy) {
            if (oldPolicy != null && newPolicy != null) {
                oldPolicy.setLoadBalancePolicy(newPolicy.getLoadBalancePolicy());
                oldPolicy.setClusterPolicy(newPolicy.getClusterPolicy());
                oldPolicy.setHealthPolicy(newPolicy.getHealthPolicy());
                oldPolicy.setRateLimitPolicies(newPolicy.getRateLimitPolicies());
                oldPolicy.setConcurrencyLimitPolicies(newPolicy.getConcurrencyLimitPolicies());
                oldPolicy.setLoadLimitPolicies(newPolicy.getLoadLimitPolicies());
                oldPolicy.setRoutePolicies(newPolicy.getRoutePolicies());
                oldPolicy.setLanePolicies(newPolicy.getLanePolicies());
                oldPolicy.setCircuitBreakPolicies(newPolicy.getCircuitBreakPolicies());
                oldPolicy.setPermissionPolicies(newPolicy.getPermissionPolicies());
                oldPolicy.setFaultInjectionPolicies(newPolicy.getFaultInjectionPolicies());
            }
        }

        @Override
        public void onDelete(Service service) {
            if (service != null) {
                service.setAuthorized(null);
                service.setAuthPolicy(null);
                service.setAuthPolicies(null);
            }
        }

        @Override
        public void onUpdate(Service oldService, Service newService) {
            if (oldService != null && newService != null) {
                oldService.setAuthorized(newService.getAuthorized());
                oldService.setAuthPolicy(newService.getAuthPolicy());
                oldService.setAuthPolicies(newService.getAuthPolicies());
            }
        }
    },

    /**
     * Represents a synchronization policy that includes only service live policies.
     */
    LIVE {
        @Override
        public void onAdd(ServicePolicy newPolicy) {
            FLOW_CONTROL.onDelete(newPolicy);
        }

        @Override
        public void onDelete(ServicePolicy oldPolicy) {
            if (oldPolicy != null) {
                oldPolicy.setLivePolicy(null);
            }
        }

        @Override
        public void onUpdate(ServicePolicy oldPolicy, ServicePolicy newPolicy) {
            if (oldPolicy != null && newPolicy != null) {
                oldPolicy.setLivePolicy(newPolicy.getLivePolicy());
            }
        }

        @Override
        public void onAdd(Service service) {
            FLOW_CONTROL.onDelete(service);
        }
    }
}
