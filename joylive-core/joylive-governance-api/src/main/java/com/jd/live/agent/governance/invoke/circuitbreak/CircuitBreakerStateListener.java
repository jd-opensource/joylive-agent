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
 * CircuitBreakerStateListener
 *
 * @since 1.1.0
 */
public interface CircuitBreakerStateListener {

    /**
     * <p>Observer method triggered when circuit breaker state changed. The transformation could be:</p>
     * <ul>
     * <li>From {@code CLOSED} to {@code OPEN} (with the triggered metric)</li>
     * <li>From {@code OPEN} to {@code HALF_OPEN}</li>
     * <li>From {@code OPEN} to {@code CLOSED}</li>
     * <li>From {@code HALF_OPEN} to {@code OPEN} (with the triggered metric)</li>
     * </ul>
     *
     * @param event state change event
     */
    void onStateChange(CircuitBreakerStateChangeEvent event);

}
