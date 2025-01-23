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
     * Returns the current status of the circuit breaker, including its phase and recover ratio (if applicable).
     *
     * @param now the current time in milliseconds since the epoch
     * @return the current status of the circuit breaker
     */
    CircuitBreakInfo getInfo(long now);
}
