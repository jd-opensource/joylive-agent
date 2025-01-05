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

/**
 * CircuitBreaker States
 *
 * @since 1.1.0
 */

/**
 * Represents the different states of a circuit breaker.
 */
public enum CircuitBreakerState {

    /**
     * The circuit breaker is in the open state, blocking requests and returning a failure response.
     */
    OPEN,

    /**
     * The circuit breaker is in the half-open state, allowing a limited number of test requests to determine if the system has recovered.
     */
    HALF_OPEN,

    /**
     * The circuit breaker is in the closed state, allowing requests to pass through.
     */
    CLOSED,

    /**
     * The circuit breaker is in the disabled state, where it does not perform any circuit breaker functionality and allows all requests to pass through.
     */
    DISABLED
}
