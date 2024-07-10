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

import com.jd.live.agent.governance.invoke.circuitbreak.CircuitBreakerState;
import com.jd.live.agent.governance.invoke.circuitbreak.CircuitBreakerStateEvent;
import com.jd.live.agent.governance.invoke.circuitbreak.CircuitBreakerStateListener;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.event.CircuitBreakerOnStateTransitionEvent;
import io.github.resilience4j.core.EventConsumer;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * A consumer that listens for state transition events from a Resilience4j circuit breaker
 * and notifies registered listeners of state changes.
 */
public class Resilience4jCircuitBreakerEventConsumer implements EventConsumer<CircuitBreakerOnStateTransitionEvent> {

    private final List<CircuitBreakerStateListener> listeners = new CopyOnWriteArrayList<>();

    @Override
    public void consumeEvent(CircuitBreakerOnStateTransitionEvent event) {
        CircuitBreaker.StateTransition transition = event.getStateTransition();
        CircuitBreakerStateEvent e = new CircuitBreakerStateEvent();
        e.setUri(event.getCircuitBreakerName());
        e.setFrom(convertState(transition.getFromState()));
        e.setTo(convertState(transition.getToState()));
        for (CircuitBreakerStateListener listener : listeners) {
            listener.onStateChange(e);
        }
    }

    /**
     * Adds a listener to be notified of circuit breaker state changes.
     *
     * @param listener the listener to add. If the listener is null, it will not be added.
     */
    public void addListener(CircuitBreakerStateListener listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }

    /**
     * Converts a Resilience4j circuit breaker state to a custom circuit breaker state.
     *
     * @param state the Resilience4j circuit breaker state to convert.
     * @return the corresponding custom circuit breaker state.
     */
    private CircuitBreakerState convertState(CircuitBreaker.State state) {
        switch (state) {
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

}
