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

/**
 * Represents the current status of a circuit breaker.
 */
@Getter
public class CircuitBreakInfo {

    /**
     * The current phase of the circuit breaker.
     */
    private final CircuitBreakPhase phase;

    /**
     * The ratio of successful requests required for the circuit breaker to transition from the recover phase to the closed phase.
     * This value is only applicable when the circuit breaker is in the recover phase.
     */
    private final Double recoverRatio;

    /**
     * Creates a new instance of CircuitBreakStatus with the specified phase.
     *
     * @param phase the current phase of the circuit breaker
     */
    public CircuitBreakInfo(CircuitBreakPhase phase) {
        this(phase, null);
    }

    /**
     * Creates a new instance of CircuitBreakStatus with the specified phase and recover ratio.
     *
     * @param phase        the current phase of the circuit breaker
     * @param recoverRatio the ratio of successful requests required for the circuit breaker to transition from the recover phase to the
     *                     closed phase
     */
    public CircuitBreakInfo(CircuitBreakPhase phase, Double recoverRatio) {
        this.phase = phase;
        this.recoverRatio = recoverRatio;
    }
}
