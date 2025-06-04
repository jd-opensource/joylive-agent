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
package com.jd.live.agent.governance.invoke.fault;

import com.jd.live.agent.core.extension.annotation.Extensible;
import com.jd.live.agent.governance.invoke.auth.Permission;
import com.jd.live.agent.governance.policy.service.fault.FaultInjectionPolicy;

import java.util.Random;

/**
 * Represents an interface for fault injection mechanisms.
 * Fault injection is used to simulate or introduce failures in a system to test its resilience and behavior under adverse conditions.
 * Implementations of this interface should define how permits are acquired based on a fault injection policy.
 */
@Extensible
public interface FaultInjection {

    /**
     * Attempts to acquire a fault injection permit based on the given policy.
     * Uses random sampling to determine permit availability.
     *
     * @param policy fault injection rules to evaluate
     * @param random random number generator for sampling decisions
     * @return permit acquisition result (success/failure)
     */
    Permission acquire(FaultInjectionPolicy policy, Random random);
}
