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
package com.jd.live.agent.governance.policy.service.circuitbreak;

import com.jd.live.agent.governance.policy.PolicyId;
import com.jd.live.agent.governance.policy.PolicyInherit;
import lombok.Getter;
import lombok.Setter;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * CircuitBreakerPolicy
 *
 * @since 1.1.0
 */
@Setter
@Getter
public class CircuitBreakerPolicy extends PolicyId implements PolicyInherit.PolicyInheritWithIdGen<CircuitBreakerPolicy> {

    public static final String DEFAULT_CIRCUIT_BREAKER_TYPE = "Resilience4j";

    /**
     * Name of this policy
     */
    private String name;

    /**
     * Implementation types of circuit-breaker
     */
    private String realizeType;

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
     * Whether to force the circuit breaker to be turned on
     */
    private boolean forceOpen = false;

    /**
     * Downgrade configuration
     */
    private DegradeConfig degradeConfig;

    /**
     * The version of the policy
     */
    private long version;

    /**
     * Map of temporarily blocked endpoints, key is endpoint id and value is the end time of block
     */
    private Map<String, Long> broken = new ConcurrentHashMap<>();

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
        if (source.getVersion() == version) {
            broken = source.broken;
        }
    }

    /**
     * Checks if the specified error code is present in the list of error codes.
     *
     * @param errorCode the error code to check.
     * @return {@code true} if the error code is present, {@code false} otherwise.
     */
    public boolean containsError(String errorCode) {
        return errorCode != null && errorCodes != null && errorCodes.contains(errorCode);
    }

    /**
     * Checks if the circuit for the given ID is currently broken.
     *
     * @param id  the identifier of the circuit.
     * @param now the current time in milliseconds.
     * @return {@code true} if the circuit is broken, {@code false} otherwise.
     */
    public boolean isBroken(String id, long now) {
        Long endTime = id == null ? null : broken.get(id);
        if (endTime == null) {
            return false;
        }
        if (endTime <= now) {
            broken.remove(id);
            return false;
        }
        return true;
    }

    /**
     * Adds an entry to the broken circuits with the specified ID and timestamp.
     *
     * @param id  the identifier of the circuit.
     * @param now the current time in milliseconds when the circuit was broken.
     */
    public void addBroken(String id, long now) {
        if (id != null) {
            broken.put(id, now);
        }
    }

    /**
     * Removes the entry of the broken circuit with the specified ID.
     *
     * @param id the identifier of the circuit to remove.
     */
    public void removeBroken(String id) {
        if (id != null) {
            broken.remove(id);
        }
    }

}
