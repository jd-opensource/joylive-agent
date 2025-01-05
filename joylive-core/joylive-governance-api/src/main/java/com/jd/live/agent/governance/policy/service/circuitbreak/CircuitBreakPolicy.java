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

import com.jd.live.agent.governance.exception.ErrorPolicy;
import com.jd.live.agent.governance.policy.PolicyId;
import com.jd.live.agent.governance.policy.PolicyInherit;
import com.jd.live.agent.governance.policy.PolicyVersion;
import com.jd.live.agent.governance.policy.service.exception.ErrorParserPolicy;
import lombok.Getter;
import lombok.Setter;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * CircuitBreakPolicy
 *
 * @since 1.1.0
 */
@Setter
@Getter
public class CircuitBreakPolicy extends PolicyId
        implements PolicyInherit.PolicyInheritWithIdGen<CircuitBreakPolicy>, ErrorPolicy, PolicyVersion {

    public static final String SLIDING_WINDOW_TIME = "time";
    public static final String SLIDING_WINDOW_COUNT = "count";
    public static final int DEFAULT_FAILURE_RATE_THRESHOLD = 50;
    public static final int DEFAULT_SLOW_CALL_RATE_THRESHOLD = 50;
    public static final int DEFAULT_SLOW_CALL_DURATION_THRESHOLD = 10000;
    public static final int DEFAULT_WAIT_DURATION_IN_OPEN_STATE = 60;
    public static final int DEFAULT_ALLOWED_CALLS_IN_HALF_OPEN_STATE = 10;
    public static final int DEFAULT_SLIDING_WINDOW_SIZE = 100;
    public static final int DEFAULT_MIN_CALLS_THRESHOLD = 10;
    public static final int DEFAULT_INSTANCE_RECOVER_DURATION = 1000 * 15;
    public static final int DEFAULT_MAX_WAIT_DURATION_IN_HALF_OPEN_STATE = 0;

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
    private CircuitBreakLevel level = CircuitBreakLevel.INSTANCE;

    /**
     * Sliding window type (statistical window type): count, time
     */
    private String slidingWindowType = SLIDING_WINDOW_TIME;

    /**
     * Sliding window size (statistical window size)
     */
    private int slidingWindowSize = DEFAULT_SLIDING_WINDOW_SIZE;

    /**
     * Minimum request threshold
     */
    private int minCallsThreshold = DEFAULT_MIN_CALLS_THRESHOLD;

    /**
     * Code policy
     */
    private ErrorParserPolicy codePolicy;

    /**
     * Error code
     */
    private Set<String> errorCodes;

    /**
     * Error message policy
     */
    private ErrorParserPolicy messagePolicy;

    /**
     * Collection of error messages. This parameter specifies which status codes should be considered retryable.
     */
    private Set<String> errorMessages;

    /**
     * Exception full class names.
     */
    private Set<String> exceptions;

    /**
     * Failure rate threshold
     */
    private float failureRateThreshold = DEFAULT_FAILURE_RATE_THRESHOLD;

    /**
     * Threshold for slow call rate
     */
    private float slowCallRateThreshold = DEFAULT_SLOW_CALL_RATE_THRESHOLD;

    /**
     * Minimum duration for slow invocation (milliseconds)
     */
    private int slowCallDurationThreshold = DEFAULT_SLOW_CALL_DURATION_THRESHOLD;

    /**
     * Fuse time (seconds)
     */
    private int waitDurationInOpenState = DEFAULT_WAIT_DURATION_IN_OPEN_STATE;

    /**
     * In the half-open state, callable numbers
     */
    private int allowedCallsInHalfOpenState = DEFAULT_ALLOWED_CALLS_IN_HALF_OPEN_STATE;

    private int maxWaitDurationInHalfOpenState = DEFAULT_MAX_WAIT_DURATION_IN_HALF_OPEN_STATE;

    /**
     * Whether to force the circuit breaker to be turned on
     */
    private boolean forceOpen = false;

    /**
     * The gradual recovery period after the instance-level circuit breaker is opened.
     */
    private int recoveryDuration = DEFAULT_INSTANCE_RECOVER_DURATION;

    /**
     * Downgrade configuration
     */
    private DegradeConfig degradeConfig;

    /**
     * The version of the policy
     */
    private long version;

    /**
     * Map of temporarily blocked endpoints
     */
    private transient Map<String, CircuitBreakEndpoint> endpoints = new ConcurrentHashMap<>();

    @Override
    public void supplement(CircuitBreakPolicy source) {
        if (source == null) {
            return;
        }
        if (version <= 0) {
            name = source.getName();
            realizeType = source.getRealizeType();
            level = source.getLevel();
            version = source.getVersion();
            slidingWindowType = source.getSlidingWindowType();
            slidingWindowSize = source.getSlidingWindowSize();
            minCallsThreshold = source.getMinCallsThreshold();
            codePolicy = source.getCodePolicy() == null ? null : source.getCodePolicy().clone();
            if (errorCodes == null && source.getErrorCodes() != null) {
                errorCodes = new HashSet<>(source.getErrorCodes());
            }
            messagePolicy = source.getMessagePolicy() == null ? null : source.getMessagePolicy().clone();
            if (errorMessages == null && source.getErrorMessages() != null) {
                errorMessages = new HashSet<>(source.getErrorMessages());
            }
            if (exceptions == null && source.getExceptions() != null) {
                exceptions = new HashSet<>(source.getExceptions());
            }
            failureRateThreshold = source.getFailureRateThreshold();
            slowCallRateThreshold = source.getSlowCallRateThreshold();
            slowCallDurationThreshold = source.getSlowCallDurationThreshold();
            waitDurationInOpenState = source.getWaitDurationInOpenState();
            allowedCallsInHalfOpenState = source.getAllowedCallsInHalfOpenState();
            forceOpen = source.isForceOpen();
            if (degradeConfig == null && source.getDegradeConfig() != null) {
                degradeConfig = new DegradeConfig(source.getDegradeConfig());
            }
            id = source.getId();
            uri = source.getUri();
        }
        if (source.getVersion() == version) {
            endpoints = source.endpoints;
        }
    }

    @Override
    public boolean isEnabled() {
        return (errorCodes != null && !errorCodes.isEmpty() || exceptions != null && !exceptions.isEmpty());
    }

    @Override
    public boolean containsErrorCode(String errorCode) {
        return errorCode != null && errorCodes != null && errorCodes.contains(errorCode);
    }

    @Override
    public boolean containsException(String className) {
        return className != null && exceptions != null && exceptions.contains(className);
    }

    @Override
    public boolean containsException(Set<String> classNames) {
        return ErrorPolicy.containsException(classNames, exceptions);
    }

    /**
     * Retrieves the circuit break endpoint by its ID.
     *
     * @param id the identifier of the circuit break endpoint
     * @return the circuit break endpoint, or null if not found or ID is null
     */
    public CircuitBreakEndpoint getEndpoint(String id) {
        return id == null ? null : endpoints.get(id);
    }

    /**
     * Adds an endpoint to the circuit breaker's list of broken endpoints.
     *
     * @param endpoint The endpoint to add. If the endpoint is null, it will not be added.
     */
    public void addEndpoint(CircuitBreakEndpoint endpoint) {
        if (endpoint != null) {
            endpoints.put(endpoint.getId(), endpoint);
        }
    }

    /**
     * Updates the state of the specified endpoint.
     *
     * @param id    The unique identifier of the endpoint to update.
     * @param state The new state to set for the endpoint.
     */
    public void updateEndpoint(String id, CircuitBreakEndpointState state) {
        CircuitBreakEndpoint endpoint = getEndpoint(id);
        if (endpoint != null) {
            endpoint = endpoint.clone();
            endpoint.setState(state);
            endpoint.setLastUpdateTime(System.currentTimeMillis());
            endpoints.put(id, endpoint);
        }
    }

    /**
     * Removes the endpoint with the specified ID from the collection if it exists.
     *
     * @param id The ID of the endpoint to be removed.
     */
    public void removeEndpoint(String id) {
        if (id != null) {
            endpoints.remove(id);
        }
    }

    /**
     * Removes the specified endpoint if it exists.
     *
     * @param endpoint The endpoint to be removed.
     */
    public void removeEndpoint(CircuitBreakEndpoint endpoint) {
        if (endpoint != null) {
            endpoints.computeIfPresent(endpoint.getId(), (k, v) -> v == endpoint ? null : v);
        }
    }

    public void cache() {
        if (codePolicy != null) {
            codePolicy.cache();
        }
    }

}
