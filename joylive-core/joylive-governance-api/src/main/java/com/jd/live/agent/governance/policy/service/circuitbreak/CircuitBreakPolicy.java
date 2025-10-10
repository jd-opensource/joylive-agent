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
import com.jd.live.agent.governance.util.RecoverRatio;
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
public class CircuitBreakPolicy extends PolicyId
        implements PolicyInherit.PolicyInheritWithIdGen<CircuitBreakPolicy>, ErrorPolicy, PolicyVersion {

    public static final String DEFAULT_SLIDING_WINDOW_TIME = "time";
    public static final String SLIDING_WINDOW_COUNT = "count";
    public static final float DEFAULT_FAILURE_RATE_THRESHOLD = 50F;
    public static final float DEFAULT_SLOW_CALL_RATE_THRESHOLD = 50F;
    public static final int DEFAULT_SLOW_CALL_DURATION_THRESHOLD = 10000;
    public static final int DEFAULT_WAIT_DURATION_IN_OPEN_STATE = 60;
    public static final int DEFAULT_ALLOWED_CALLS_IN_HALF_OPEN_STATE = 10;
    public static final int DEFAULT_SLIDING_WINDOW_SIZE = 100;
    public static final int DEFAULT_MIN_CALLS_THRESHOLD = 10;
    public static final int DEFAULT_OUTLIER_MAX_PERCENT = 50;
    public static final int DEFAULT_RECOVER_DURATION = 1000 * 15;
    public static final int DEFAULT_MAX_WAIT_DURATION_IN_HALF_OPEN_STATE = 0;
    public static final int DEFAULT_RECOVER_PHASE = 10;

    /**
     * Name of this policy
     */
    @Setter
    @Getter
    private String name;

    /**
     * Implementation types of circuit-breaker
     */
    @Setter
    @Getter
    private String realizeType;

    /**
     * Level of circuit breaker policy
     */
    @Setter
    private CircuitBreakLevel level;

    /**
     * Sliding window type (statistical window type): count, time
     */
    @Setter
    private String slidingWindowType;

    /**
     * Sliding window size (statistical window size)
     */
    @Setter
    private Integer slidingWindowSize;

    /**
     * Minimum request threshold
     */
    @Setter
    private Integer minCallsThreshold;

    @Setter
    private Integer outlierMaxPercent;

    /**
     * Code policy
     */
    @Setter
    @Getter
    private ErrorParserPolicy codePolicy;

    /**
     * Error code
     */
    @Setter
    @Getter
    private Set<String> errorCodes;

    /**
     * Error message policy
     */
    @Setter
    @Getter
    private ErrorParserPolicy messagePolicy;

    /**
     * Collection of error messages. This parameter specifies which status codes should be considered retryable.
     */
    @Setter
    @Getter
    private Set<String> errorMessages;

    /**
     * Exception full class names.
     */
    @Setter
    @Getter
    private Set<String> exceptions;

    /**
     * Failure rate threshold
     */
    @Setter
    private Float failureRateThreshold;

    /**
     * Threshold for slow call rate
     */
    @Setter
    private Float slowCallRateThreshold;

    /**
     * Minimum duration for slow invocation (milliseconds)
     */
    @Setter
    private Integer slowCallDurationThreshold;

    /**
     * Fuse time (seconds)
     */
    @Setter
    private Integer waitDurationInOpenState;

    /**
     * In the half-open state, callable numbers
     */
    @Setter
    private Integer allowedCallsInHalfOpenState;

    @Setter
    private Integer maxWaitDurationInHalfOpenState;

    /**
     * Whether to force the circuit breaker to be turned on
     */
    @Setter
    private Boolean forceOpen;

    /**
     * Indicates whether the recovery mechanism is enabled.
     */
    @Setter
    private Boolean recoveryEnabled;

    /**
     * The duration in milliseconds for which the recovery mechanism is active.
     * Defaults to {@link #DEFAULT_RECOVER_DURATION}.
     */
    @Setter
    private Integer recoveryDuration;

    /**
     * The number of phases in the recovery mechanism.
     * Defaults to {@link #DEFAULT_RECOVER_PHASE}.
     */
    @Setter
    private Integer recoveryPhase;

    /**
     * Downgrade configuration
     */
    @Setter
    @Getter
    private DegradeConfig degradeConfig;

    /**
     * The version of the policy
     */
    @Setter
    @Getter
    private long version;

    private transient RecoverRatio recoverRatio;

    /**
     * Map of temporarily blocked endpoints
     */
    private transient Map<String, CircuitBreakInspector> inspectors = new ConcurrentHashMap<>();

    public CircuitBreakPolicy() {
    }

    public CircuitBreakPolicy(String name) {
        this.name = name;
    }

    public CircuitBreakLevel getLevel() {
        return level == null ? CircuitBreakLevel.INSTANCE : level;
    }

    public String getSlidingWindowType() {
        return slidingWindowType == null || slidingWindowType.isEmpty() ? DEFAULT_SLIDING_WINDOW_TIME : slidingWindowType;
    }

    public int getSlidingWindowSize() {
        return slidingWindowSize == null || slidingWindowSize <= 0 ? DEFAULT_SLIDING_WINDOW_SIZE : slidingWindowSize;
    }

    public int getMinCallsThreshold() {
        return minCallsThreshold == null || minCallsThreshold <= 0 ? DEFAULT_MIN_CALLS_THRESHOLD : minCallsThreshold;
    }

    public int getOutlierMaxPercent() {
        return outlierMaxPercent == null || outlierMaxPercent <= 0 ? DEFAULT_OUTLIER_MAX_PERCENT : Math.min(outlierMaxPercent, 100);
    }

    public float getFailureRateThreshold() {
        return failureRateThreshold == null || failureRateThreshold <= 0 ? DEFAULT_FAILURE_RATE_THRESHOLD : Math.min(failureRateThreshold, 100);
    }

    public float getSlowCallRateThreshold() {
        return slowCallRateThreshold == null || slowCallRateThreshold <= 0 ? DEFAULT_SLOW_CALL_RATE_THRESHOLD : Math.min(slowCallRateThreshold, 100);
    }

    public int getSlowCallDurationThreshold() {
        return slowCallDurationThreshold == null || slowCallDurationThreshold <= 0 ? DEFAULT_SLOW_CALL_DURATION_THRESHOLD : slowCallDurationThreshold;
    }

    public int getAllowedCallsInHalfOpenState() {
        return allowedCallsInHalfOpenState == null || allowedCallsInHalfOpenState <= 0 ? DEFAULT_ALLOWED_CALLS_IN_HALF_OPEN_STATE : allowedCallsInHalfOpenState;
    }

    public int getMaxWaitDurationInHalfOpenState() {
        return maxWaitDurationInHalfOpenState == null || maxWaitDurationInHalfOpenState <= 0 ? DEFAULT_MAX_WAIT_DURATION_IN_HALF_OPEN_STATE : maxWaitDurationInHalfOpenState;
    }

    public boolean isForceOpen() {
        return forceOpen != null && forceOpen;
    }

    public boolean isRecoveryEnabled() {
        return recoveryEnabled != null && recoveryEnabled;
    }

    public int getWaitDurationInOpenState() {
        return waitDurationInOpenState == null || waitDurationInOpenState <= 0 ? DEFAULT_WAIT_DURATION_IN_OPEN_STATE : waitDurationInOpenState;
    }

    public int getRecoveryDuration() {
        return recoveryDuration == null || recoveryDuration <= 0 ? DEFAULT_RECOVER_DURATION : recoveryDuration;
    }

    public int getRecoveryPhase() {
        return recoveryPhase == null || recoveryPhase <= 0 ? DEFAULT_RECOVER_PHASE : recoveryPhase;
    }

    @Override
    public void supplement(CircuitBreakPolicy source) {
        if (source == null) {
            return;
        }
        if (name == null) {
            name = source.getName();
        }
        if (realizeType == null) {
            realizeType = source.getRealizeType();
        }
        if (level == null) {
            level = source.getLevel();
        }
        if (slidingWindowType == null) {
            slidingWindowType = source.slidingWindowType;
        }
        if (slidingWindowSize == null) {
            slidingWindowSize = source.slidingWindowSize;
        }
        if (minCallsThreshold == null) {
            minCallsThreshold = source.minCallsThreshold;
        }
        if (outlierMaxPercent == null) {
            outlierMaxPercent = source.outlierMaxPercent;
        }
        if (codePolicy == null) {
            codePolicy = source.codePolicy == null ? null : source.codePolicy.clone();
        }
        if (errorCodes == null) {
            errorCodes = source.errorCodes == null ? null : new HashSet<>(source.errorCodes);
        }
        if (messagePolicy == null) {
            messagePolicy = source.messagePolicy == null ? null : source.messagePolicy.clone();
        }
        if (errorMessages == null) {
            errorMessages = source.errorMessages == null ? null : new HashSet<>(source.errorMessages);
        }
        if (exceptions == null) {
            exceptions = source.exceptions == null ? null : new HashSet<>(source.exceptions);
        }
        if (failureRateThreshold == null) {
            failureRateThreshold = source.failureRateThreshold;
        }
        if (slowCallRateThreshold == null) {
            slowCallRateThreshold = source.slowCallRateThreshold;
        }
        if (slowCallDurationThreshold == null) {
            slowCallDurationThreshold = source.slowCallDurationThreshold;
        }
        if (waitDurationInOpenState == null) {
            waitDurationInOpenState = source.waitDurationInOpenState;
        }
        if (allowedCallsInHalfOpenState == null) {
            allowedCallsInHalfOpenState = source.allowedCallsInHalfOpenState;
        }
        if (forceOpen == null) {
            forceOpen = source.forceOpen;
        }
        if (degradeConfig == null) {
            degradeConfig = source.degradeConfig == null ? null : new DegradeConfig(source.getDegradeConfig());
        }
        if (recoveryEnabled == null) {
            recoveryEnabled = source.recoveryEnabled;
        }
        if (recoveryDuration == null) {
            recoveryDuration = source.recoveryDuration;
        }
        if (recoveryPhase == null) {
            recoveryPhase = source.recoveryPhase;
        }
        if (version <= 0) {
            version = source.getVersion();
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
     * Determines whether the system should be in protect mode based on the number of instances and the configured outlier ratio.
     *
     * @param instances The number of instances to evaluate.
     * @return {@code true} if protect mode should be enabled, {@code false} otherwise.
     */
    public boolean isProtectMode(int instances) {
        if (level == null || !level.isProtectionSupported()) {
            return false;
        }
        // the ratio is greater than zero.
        double ratio = getOutlierMaxPercent();
        // The number of instances cannot exceed the maximum limit.
        int max = (int) Math.floor(instances * ratio / 100);
        // The number of instances plus the current request
        int count = inspectors.size() + 1;
        return count > max;
    }

    /**
     * Retrieves the circuit break inspector by its ID.
     *
     * @param id the identifier of the circuit break inspector
     * @return the circuit break inspector, or null if not found or ID is null
     */
    public CircuitBreakInspector getInspector(String id) {
        return id == null ? null : inspectors.get(id);
    }

    /**
     * Adds a new inspector with the specified ID and state to the circuit breaker.
     *
     * @param id        the unique identifier of the inspector
     * @param inspector the circuit breaker inspector
     */
    public void addInspector(String id, CircuitBreakInspector inspector) {
        if (id != null && inspector != null) {
            inspectors.put(id, inspector);
        }
    }

    /**
     * Removes the specified inspector if it exists.
     *
     * @param id        the ID of the inspector to be removed
     * @param inspector the circuit breaker inspector to be removed
     */
    public void removeInspector(String id, CircuitBreakInspector inspector) {
        if (id != null) {
            inspectors.computeIfPresent(id, (k, v) -> v == inspector ? null : v);
        }
    }

    /**
     * Exchanges the current policy of the circuit breaker with the specified policy
     * if the new policy is not null, not the same as the current policy, and has the same version.
     *
     * @param policy the new policy to be set for the circuit breaker
     */
    public void exchange(CircuitBreakPolicy policy) {
        if (inspectors != policy.inspectors) {
            inspectors = policy.inspectors;
        }
    }

    /**
     * Calculates the recovery ratio for a given duration.
     *
     * @param duration The duration in milliseconds for which the recovery ratio is calculated.
     * @return The recovery ratio as a double value if the internal RecoverRatio instance is not null
     * and the duration is less than the recovery period. Returns {@code null} if the internal
     * RecoverRatio instance is null or if the duration is greater than or equal to the recovery period.
     */
    public Double getRecoveryRatio(long duration) {
        return recoverRatio == null ? null : recoverRatio.getRatio(duration);
    }

    public void cache() {
        if (degradeConfig != null) {
            degradeConfig.cache();
        }
        recoverRatio = isRecoveryEnabled() ? new RecoverRatio(getRecoveryDuration(), getRecoveryPhase()) : null;
    }

}
