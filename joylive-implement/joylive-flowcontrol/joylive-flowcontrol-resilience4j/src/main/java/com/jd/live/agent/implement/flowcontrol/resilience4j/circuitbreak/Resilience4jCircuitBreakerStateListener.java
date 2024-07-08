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
import com.jd.live.agent.governance.invoke.circuitbreak.CircuitBreakerStateEvent;
import com.jd.live.agent.governance.invoke.circuitbreak.CircuitBreakerStateListener;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.event.CircuitBreakerEvent;
import io.github.resilience4j.circuitbreaker.event.CircuitBreakerOnStateTransitionEvent;
import io.github.resilience4j.core.EventConsumer;

/**
 * Resilience4jCircuitBreakerStateListener
 *
 * @since 1.1.0
 */
public abstract class Resilience4jCircuitBreakerStateListener implements CircuitBreakerStateListener, EventConsumer<CircuitBreakerEvent> {

    private static final Logger logger = LoggerFactory.getLogger(Resilience4jCircuitBreakerStateListener.class);

    @Override
    public void consumeEvent(CircuitBreakerEvent event) {
        CircuitBreakerStateEvent e = new CircuitBreakerStateEvent();
        e.setUri(event.getCircuitBreakerName());
        if (event instanceof CircuitBreakerOnStateTransitionEvent) {
            CircuitBreakerOnStateTransitionEvent stateTransitionEvent = (CircuitBreakerOnStateTransitionEvent) event;
            e.setFrom(convertState(stateTransitionEvent.getStateTransition().getFromState()));
            e.setTo(convertState(stateTransitionEvent.getStateTransition().getToState()));
            onStateChange(e);
            if (logger.isDebugEnabled()) {
                logger.debug("[CircuitBreak]CircuitBreaker State transition event: from" + stateTransitionEvent.getStateTransition().getFromState()
                        + " to " + stateTransitionEvent.getStateTransition().getToState() + ", name: " + event.getCircuitBreakerName());
            }
        }
    }

    public static CircuitBreakerState convertState(CircuitBreaker.State resilienceState) {
        switch (resilienceState) {
            case OPEN:
            case FORCED_OPEN:
                return CircuitBreakerState.OPEN;
            case HALF_OPEN:
                return CircuitBreakerState.HALF_OPEN;
            case CLOSED:
            case METRICS_ONLY:
            case DISABLED:
            default:
                return CircuitBreakerState.CLOSED;
        }
    }

    /**
     * <p>Observer method triggered when circuit breaker state changed. The transformation could be:</p>
     * <ul>
     * <li>From {@code CLOSED} to {@code OPEN} (with the triggered metric)</li>
     * <li>From {@code OPEN} to {@code HALF_OPEN}</li>
     * <li>From {@code OPEN} to {@code CLOSED}</li>
     * <li>From {@code HALF_OPEN} to {@code OPEN} (with the triggered metric)</li>
     * </ul>
     *  @param event state change event
     */
    @Override
    public abstract void onStateChange(CircuitBreakerStateEvent event);

}
