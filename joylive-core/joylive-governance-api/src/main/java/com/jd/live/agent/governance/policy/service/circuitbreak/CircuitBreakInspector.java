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

/**
 * Interface for inspecting the state and recovery status of a circuit breaker.
 */
public interface CircuitBreakInspector {

    /**
     * Checks if the circuit breaker is in the open state and the current time is within the end time.
     *
     * @param now The current timestamp in milliseconds.
     * @return {@code true} if the circuit breaker is in the open state and
     * the current time is less than or equal to the end time, {@code false} otherwise.
     */
    boolean isOpen(long now);

    /**
     * Checks if the circuit breaker is in the half-open state.
     *
     * @param now The current timestamp in milliseconds.
     * @return {@code true} if the circuit breaker is in the half-open state, {@code false} otherwise.
     */
    boolean isHalfOpen(long now);

    /**
     * Checks if the circuit breaker is in the closed state and has not recovered within the specified duration.
     *
     * @param now The current timestamp in milliseconds.
     * @return {@code true} if the circuit breaker is in the closed state and has not recovered within the specified duration, {@code false} otherwise.
     */
    boolean isRecover(long now);

    /**
     * Retrieves the recovery ratio for the circuit breaker at the specified time.
     *
     * @param now the current time in milliseconds
     * @return the recovery ratio as a double value
     */
    Double getRecoverRatio(long now);
}
