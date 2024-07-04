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
package com.jd.live.agent.implement.flowcontrol.resilience4j.circuitbreak;

import com.jd.live.agent.bootstrap.logger.Logger;
import com.jd.live.agent.bootstrap.logger.LoggerFactory;
import com.jd.live.agent.governance.invoke.circuitbreak.CircuitBreakerState;
import com.jd.live.agent.governance.invoke.circuitbreak.CircuitBreakerStateChangeEvent;

/**
 * InstanceCircuitBreakerStateListener
 *
 * @since 1.1.0
 */
public class InstanceCircuitBreakerStateListener extends Resilience4jCircuitBreakerStateListener {

    private static final Logger logger = LoggerFactory.getLogger(InstanceCircuitBreakerStateListener.class);

    private final String name;

    public InstanceCircuitBreakerStateListener(String name) {
        this.name = name;
    }

    /**
     * Return a name for listener
     *
     * @return name string
     */
    @Override
    public String getName() {
        return name;
    }

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
    @Override
    public void onStateChange(CircuitBreakerStateChangeEvent event) {
        if (event.getTo() == CircuitBreakerState.OPEN) {
            if (logger.isInfoEnabled()) {
                logger.info("[JMSF][CircuitBreakerStateListener]This resource will be degraded! resourceName={}", event.getResourceName());
            }
            //TODO 禁用某实例
        }
        if (event.getFrom() == CircuitBreakerState.OPEN) {
            if (logger.isInfoEnabled()) {
                logger.info("[JMSF][CircuitBreakerStateListener]This resource will be recover! resourceName={}", event.getResourceName());
            }
            //TODO 解禁某实例
        }
    }
}
