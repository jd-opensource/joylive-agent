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
package com.jd.live.agent.governance.policy.service.loadbalance;

import com.jd.live.agent.governance.policy.PolicyInherit.PolicyInheritWithId;
import com.jd.live.agent.governance.policy.service.annotation.Consumer;
import lombok.Getter;
import lombok.Setter;

/**
 * Represents a policy for load balancing that dictates how tasks or requests are distributed
 * among various resources or services. This class encapsulates the settings and strategies
 * used to balance load, aiming to optimize resource use, maximize throughput, minimize response
 * time, and avoid overloading any single resource.
 * <p>
 * The {@code LoadBalancePolicy} class implements {@link PolicyInheritWithId} interface, enabling
 * policy inheritance. This allows a load balance policy to supplement its configuration from another
 * policy of the same type. This feature is particularly useful for dynamically adjusting load balancing
 * strategies in response to changing operational conditions or requirements.
 * </p>
 *
 * Instances of this class can be identified by a unique ID and categorized by a policy type, which
 * describes the load balancing strategy (e.g., round-robin, least connections, hash-based) being applied.
 *
 * @since 1.0.0
 */
@Getter
@Setter
@Consumer
public class LoadBalancePolicy implements PolicyInheritWithId<LoadBalancePolicy> {

    /**
     * The unique identifier of the load balance policy. This ID is used to reference and manage
     * the policy within a system.
     */
    private Long id;

    /**
     * The type of load balancing policy. This string identifies the strategy used for distributing
     * load among resources or services. Examples include "round-robin", "least-connections", and
     * "hash-based" among others.
     */
    private String policyType;

    private Integer maxCandidates;

    /**
     * sticky type
     */
    private StickyType stickyType = StickyType.NONE;

    /**
     * Constructs a new, empty {@code LoadBalancePolicy}.
     */
    public LoadBalancePolicy() {
    }

    /**
     * Constructs a new {@code LoadBalancePolicy} with a specified policy type.
     *
     * @param policyType the type of load balancing strategy to be applied by this policy
     */
    public LoadBalancePolicy(String policyType) {
        this.policyType = policyType;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void supplement(LoadBalancePolicy source) {
        if (source == null) {
            return;
        }
        if (id == null) {
            id = source.getId();
        }
        if (policyType == null) {
            policyType = source.policyType;
        }
        if (stickyType == null) {
            stickyType = source.stickyType;
        }
        if (maxCandidates == null) {
            maxCandidates = source.maxCandidates;
        }
    }

}

