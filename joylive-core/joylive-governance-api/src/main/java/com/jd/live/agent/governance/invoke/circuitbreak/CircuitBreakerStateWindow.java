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
package com.jd.live.agent.governance.invoke.circuitbreak;

import lombok.Getter;

/**
 * Represents a time window for the state of a circuit breaker.
 * This class encapsulates the state of the circuit breaker along with its start and end times.
 */
@Getter
public class CircuitBreakerStateWindow {

    /**
     * The state of the circuit breaker.
     */
    private final CircuitBreakerState state;

    /**
     * The start time of the state window in milliseconds.
     */
    private final long startTime;

    /**
     * The end time of the state window in milliseconds.
     * This can be {@code null} if the state window does not have an end time.
     */
    private final Long endTime;

    /**
     * Constructs a new instance of {@link CircuitBreakerStateWindow}.
     *
     * @param state     the state of the circuit breaker
     * @param startTime the start time of the state window in milliseconds
     * @param endTime   the end time of the state window in milliseconds, can be {@code null}
     */
    public CircuitBreakerStateWindow(CircuitBreakerState state, long startTime, Long endTime) {
        this.state = state;
        this.startTime = startTime;
        this.endTime = endTime;
    }
}

