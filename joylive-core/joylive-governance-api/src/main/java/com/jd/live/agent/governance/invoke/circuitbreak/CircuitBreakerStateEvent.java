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
import lombok.Setter;

/**
 * Represents an event that captures the state transition of a circuit breaker.
 *
 * @since 1.1.0
 */
@Setter
@Getter
public class CircuitBreakerStateEvent {

    /**
     * The URI associated with the circuit breaker.
     */
    private String uri;

    /**
     * The previous state of the circuit breaker before the transition.
     */
    private CircuitBreakerState from;

    /**
     * The new state of the circuit breaker after the transition.
     */
    private CircuitBreakerState to;

}
