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

import lombok.Getter;
import lombok.Setter;

/**
 * Represents an endpoint managed by a circuit breaker.
 */
@Getter
public class CircuitBreakEndpoint implements Cloneable {

    /**
     * The unique identifier of the endpoint.
     */
    private final String id;

    /**
     * The current state of the circuit breaker for this endpoint.
     */
    @Setter
    private CircuitBreakEndpointState state;

    private long endTime;

    /**
     * The timestamp of the last update to the circuit breaker state.
     */
    @Setter
    private long lastUpdateTime;

    /**
     * Constructs a new CircuitBreakEndpoint with the specified parameters.
     *
     * @param id             the unique identifier of the endpoint
     * @param state          the initial state of the circuit breaker
     * @param endTime        the end time in milliseconds for the circuit breaker state
     * @param lastUpdateTime the last update time in milliseconds of the endpoint
     */
    private CircuitBreakEndpoint(String id, CircuitBreakEndpointState state, long endTime, long lastUpdateTime) {
        this.id = id;
        this.state = state;
        this.endTime = endTime;
        this.lastUpdateTime = lastUpdateTime;
    }

    /**
     * Creates a new CircuitBreakEndpoint in the OPEN state with the specified parameters.
     *
     * @param id      the unique identifier of the endpoint
     * @param endTime the end time in milliseconds for the circuit breaker state
     * @return a new CircuitBreakEndpoint in the OPEN state
     */
    public static CircuitBreakEndpoint open(String id, long endTime) {
        return new CircuitBreakEndpoint(id, CircuitBreakEndpointState.OPEN, endTime, System.currentTimeMillis());
    }

    /**
     * Checks if the circuit breaker is in the open state and the current time is within the end time.
     *
     * @return {@code true} if the circuit breaker is in the open state and
     * the current time is less than or equal to the end time, {@code false} otherwise.
     */
    public boolean isOpen() {
        // The transition from Open state to half_open state will not be automatically triggered.
        // A request is needed to trigger it.
        // Therefore, it is necessary to add an expiration time check.
        return state == CircuitBreakEndpointState.OPEN && System.currentTimeMillis() <= endTime;
    }

    /**
     * Checks if the circuit breaker is in the half-open state.
     *
     * @return {@code true} if the circuit breaker is in the half-open state, {@code false} otherwise.
     */
    public boolean isHalfOpen() {
        return state == CircuitBreakEndpointState.HALF_OPEN || state == CircuitBreakEndpointState.OPEN && System.currentTimeMillis() > endTime;
    }

    /**
     * Checks if the circuit breaker is in the closed state and has not recovered within the specified duration.
     *
     * @param duration The duration in milliseconds to check for recovery.
     * @return {@code true} if the circuit breaker is in the closed state and has not recovered within the specified duration, {@code false} otherwise.
     */
    public boolean isRecover(long duration) {
        return state == CircuitBreakEndpointState.CLOSED && lastUpdateTime + duration >= System.currentTimeMillis();
    }

    @Override
    public CircuitBreakEndpoint clone() {
        try {
            return (CircuitBreakEndpoint) super.clone();
        } catch (Throwable e) {
            return new CircuitBreakEndpoint(id, state, endTime, lastUpdateTime);
        }
    }
}
