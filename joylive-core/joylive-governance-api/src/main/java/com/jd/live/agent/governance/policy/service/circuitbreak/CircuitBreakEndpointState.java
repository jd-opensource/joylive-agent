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
 * Represents the different states of a circuit breaker endpoint.
 */
public enum CircuitBreakEndpointState {

    /**
     * The circuit breaker endpoint is in the closed state, allowing requests to pass through.
     */
    CLOSED,

    /**
     * The circuit breaker endpoint is in the open state, blocking requests and returning a failure response.
     */
    OPEN,

    /**
     * The circuit breaker endpoint is in the half-open state, allowing a limited number of test requests to determine if the system has recovered.
     */
    HALF_OPEN
}

