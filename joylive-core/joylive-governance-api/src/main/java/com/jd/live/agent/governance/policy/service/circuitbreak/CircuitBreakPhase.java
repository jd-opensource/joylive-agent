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
 * Represents the different phases of a circuit breaker.
 */
public enum CircuitBreakPhase {

    /**
     * The circuit breaker is open.
     */
    OPEN,

    /**
     * The circuit breaker is in a half-open state.
     */
    HALF_OPEN,

    /**
     * The circuit breaker is in the process of recovering from a failure.
     */
    RECOVER,

    /**
     * The circuit breaker is closed.
     */
    CLOSED,
}

