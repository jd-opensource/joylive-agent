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
import lombok.Getter;
import lombok.Setter;

import java.util.Set;

/**
 * CircuitBreakerPolicy
 *
 * @since 1.1.0
 */
@Setter
@Getter
public class CircuitBreakerPolicy extends PolicyId {

    private CircuitLevel level = CircuitLevel.INSTANCE;

    /**
     * Sliding window type (statistical window type): Count, time
     */
    private String slidingWindowType;

    /**
     * Sliding window size (statistical window size)
     */
    private int slidingWindowSize = 100;

    /**
     * Minimum request threshold
     */
    private int minCallThreshold = 10;

    /**
     * Error code
     */
    private Set<Integer> errorCodes;

    /**
     * Failure rate threshold
     */
    private float failureRateThreshold = 50;

    /**
     * Threshold for slow call rate
     */
    private float slowCallRateThreshold = 50;

    /**
     * Maximum duration for slow invocation (milliseconds)
     */
    private int slowCallMaxTimeInMs = 10000;

    /**
     * Fuse time (milliseconds)
     */
    private int waitMsInOpenState = 60000;

    /**
     * In the half-open state, callable numbers
     */
    private int permittedNumberOfCallsInHalfOpenState = 10;

    /**
     * Downgrade configuration
     */
    private DegradeConfig degradeConfig;

}
