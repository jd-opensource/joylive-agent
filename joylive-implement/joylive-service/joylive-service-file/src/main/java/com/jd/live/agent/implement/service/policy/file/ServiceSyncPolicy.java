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
package com.jd.live.agent.implement.service.policy.file;

import com.jd.live.agent.governance.policy.service.PolicyMerger;
import com.jd.live.agent.governance.policy.service.ServicePolicy;

/**
 * Enumeration representing different synchronization policies for service policies.
 */
public enum ServiceSyncPolicy implements PolicyMerger {

    /**
     * Represents a synchronization policy that includes all service policies.
     */
    ALL {
        @Override
        public void onDelete(ServicePolicy oldPolicy) {
            super.onDelete(oldPolicy);
            oldPolicy.setLivePolicy(null);
        }

        @Override
        public void onUpdate(ServicePolicy oldPolicy, ServicePolicy newPolicy) {
            super.onUpdate(oldPolicy, newPolicy);
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
            super.onAdd(newPolicy);
            newPolicy.setLivePolicy(null);
        }
    };


    @Override
    public void onAdd(ServicePolicy newPolicy) {

    }

    @Override
    public void onDelete(ServicePolicy oldPolicy) {
        oldPolicy.setClusterPolicy(null);
        oldPolicy.setLanePolicies(null);
        oldPolicy.setRoutePolicies(null);
        oldPolicy.setLoadBalancePolicy(null);
        oldPolicy.setConcurrencyLimitPolicies(null);
        oldPolicy.setRateLimitPolicies(null);
    }

    @Override
    public void onUpdate(ServicePolicy oldPolicy, ServicePolicy newPolicy) {
        oldPolicy.setClusterPolicy(newPolicy.getClusterPolicy());
        oldPolicy.setLanePolicies(newPolicy.getLanePolicies());
        oldPolicy.setRoutePolicies(newPolicy.getRoutePolicies());
        oldPolicy.setLoadBalancePolicy(newPolicy.getLoadBalancePolicy());
        oldPolicy.setConcurrencyLimitPolicies(newPolicy.getConcurrencyLimitPolicies());
        oldPolicy.setRateLimitPolicies(newPolicy.getRateLimitPolicies());
    }
}
