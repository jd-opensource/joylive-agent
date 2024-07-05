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
package com.jd.live.agent.governance.policy.service.circuitbreaker;

import com.jd.live.agent.governance.policy.PolicyId;
import com.jd.live.agent.governance.policy.PolicyInherit;
import lombok.Getter;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

/**
 * CircuitBreakerPolicy
 *
 * @since 1.1.0
 */
@Setter
@Getter
public class CircuitBreakerPolicy extends PolicyId implements PolicyInherit.PolicyInheritWithIdGen<CircuitBreakerPolicy> {

    public static final String QUERY_CIRCUIT_BREAKER = "circuitBreaker";

    public static final String DEFAULT_CIRCUIT_BREAKER_TYPE = "Resilience4j";

    /**
     * Name of this policy
     */
    private String name;

    /**
     * Implementation types of circuit-breaker
     */
    private String type = DEFAULT_CIRCUIT_BREAKER_TYPE;

    /**
     * Level of circuit breaker policy
     */
    private CircuitLevel level = CircuitLevel.INSTANCE;

    /**
     * Sliding window type (statistical window type): count, time
     */
    private String slidingWindowType = "time";

    /**
     * Sliding window size (statistical window size)
     */
    private int slidingWindowSize = 100;

    /**
     * Minimum request threshold
     */
    private int minCallsThreshold = 10;

    /**
     * Error code
     */
    private Set<String> errorCodes;

    /**
     * Failure rate threshold
     */
    private float failureRateThreshold = 50;

    /**
     * Threshold for slow call rate
     */
    private float slowCallRateThreshold = 50;

    /**
     * Minimum duration for slow invocation (milliseconds)
     */
    private int slowCallDurationThreshold = 10000;

    /**
     * Fuse time (milliseconds)
     */
    private int waitDurationInOpenState = 60000;

    /**
     * In the half-open state, callable numbers
     */
    private int allowedCallsInHalfOpenState = 10;

    /**
     * Downgrade configuration
     */
    private DegradeConfig degradeConfig;

    /**
     * The version of the policy.
     */
    private long version;

    @Override
    public void supplement(CircuitBreakerPolicy source) {
        if (source == null) {
            return;
        }
        if (errorCodes == null && source.getErrorCodes() != null) {
            errorCodes = new HashSet<>(source.getErrorCodes());
        }
        if (degradeConfig == null && source.getDegradeConfig() != null) {
            degradeConfig = new DegradeConfig(source.getDegradeConfig());
        }
    }
}
