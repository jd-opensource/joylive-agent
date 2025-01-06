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
package com.jd.live.agent.implement.flowcontrol.circuitbreak.resilience4j;

import com.jd.live.agent.bootstrap.logger.Logger;
import com.jd.live.agent.bootstrap.logger.LoggerFactory;
import com.jd.live.agent.core.util.Close;
import com.jd.live.agent.core.util.URI;
import com.jd.live.agent.governance.invoke.circuitbreak.AbstractCircuitBreaker;
import com.jd.live.agent.governance.invoke.circuitbreak.CircuitBreakerState;
import com.jd.live.agent.governance.invoke.circuitbreak.CircuitBreakerStateEvent;
import com.jd.live.agent.governance.invoke.circuitbreak.CircuitBreakerStateListener;
import com.jd.live.agent.governance.policy.PolicyId;
import com.jd.live.agent.governance.policy.service.circuitbreak.CircuitBreakEndpoint;
import com.jd.live.agent.governance.policy.service.circuitbreak.CircuitBreakEndpointState;
import com.jd.live.agent.governance.policy.service.circuitbreak.CircuitBreakLevel;
import com.jd.live.agent.governance.policy.service.circuitbreak.CircuitBreakPolicy;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreaker.State;
import io.github.resilience4j.circuitbreaker.event.CircuitBreakerOnStateTransitionEvent;
import io.github.resilience4j.core.EventConsumer;
import io.github.resilience4j.core.lang.NonNull;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.jd.live.agent.governance.policy.service.circuitbreak.CircuitBreakPolicy.DEFAULT_WAIT_DURATION_IN_OPEN_STATE;

/**
 * Resilience4jCircuitBreaker
 *
 * @since 1.1.0
 */
public class Resilience4jCircuitBreaker extends AbstractCircuitBreaker {

    private final io.github.resilience4j.circuitbreaker.CircuitBreaker delegate;

    private final LiveEventConsumer eventConsumer;

    public Resilience4jCircuitBreaker(CircuitBreakPolicy policy, URI uri, CircuitBreaker delegate) {
        super(policy, uri);
        this.delegate = delegate;
        this.eventConsumer = policy.getLevel() != CircuitBreakLevel.INSTANCE
                ? new LiveEventConsumer(this.started)
                : new LiveEventConsumer(this.started, new LiveStateListener(
                policy, uri.getParameter(PolicyId.KEY_SERVICE_ENDPOINT), this.started));
        this.delegate.getEventPublisher().onStateTransition(eventConsumer);
    }

    @Override
    protected boolean doAcquire() {
        return delegate.tryAcquirePermission();
    }

    @Override
    protected void doRelease() {
        delegate.releasePermission();
    }

    @Override
    public boolean isExpired(long timeout) {
        return delegate.getState() == State.CLOSED && super.isExpired(timeout);
    }

    @Override
    protected void doOnError(long durationInMs, Throwable throwable) {
        delegate.onError(durationInMs, TimeUnit.MILLISECONDS, throwable);
    }

    @Override
    protected void doOnSuccess(long durationInMs) {
        delegate.onSuccess(durationInMs, TimeUnit.MILLISECONDS);
    }

    @Override
    public void addListener(CircuitBreakerStateListener listener) {
        eventConsumer.addListener(listener);
    }

    @Override
    protected void doClose() {
        eventConsumer.close();
    }

    /**
     * A consumer that listens for state transition events from a Resilience4j circuit breaker
     * and notifies registered listeners of state changes.
     */
    private static class LiveEventConsumer implements EventConsumer<CircuitBreakerOnStateTransitionEvent>, AutoCloseable {

        private final AtomicBoolean started;

        private final List<CircuitBreakerStateListener> listeners = new CopyOnWriteArrayList<>();

        LiveEventConsumer(AtomicBoolean started, CircuitBreakerStateListener... listeners) {
            this.started = started;
            if (listeners != null) {
                this.listeners.addAll(Arrays.asList(listeners));
            }
        }

        @Override
        public void consumeEvent(@NonNull CircuitBreakerOnStateTransitionEvent event) {
            if (!started.get()) {
                return;
            }
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
        private CircuitBreakerState convertState(State state) {
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

        @Override
        public void close() {
            Close closer = Close.instance();
            for (CircuitBreakerStateListener listener : listeners) {
                if (listener instanceof AutoCloseable) {
                    closer.close((AutoCloseable) listener);
                }
            }
        }
    }

    /**
     * LiveStateListener
     *
     * @since 1.1.0
     */
    private static class LiveStateListener implements CircuitBreakerStateListener, AutoCloseable {

        private static final Logger logger = LoggerFactory.getLogger(LiveStateListener.class);

        private final CircuitBreakPolicy policy;

        private final String instanceId;

        private final AtomicBoolean started;

        LiveStateListener(CircuitBreakPolicy policy, String instanceId, AtomicBoolean started) {
            this.policy = policy;
            this.instanceId = instanceId;
            this.started = started;
        }

        @Override
        public void onStateChange(CircuitBreakerStateEvent event) {
            if (!started.get()) {
                // avoid another breaker conflict.
                return;
            }
            if (logger.isDebugEnabled()) {
                logger.debug("[CircuitBreak]Instance state is transitioned from " + event.getFrom() + " to " + event.getTo() + ", uri=" + event.getUri());
            }
            switch (event.getTo()) {
                case CLOSED:
                    policy.updateEndpoint(instanceId, CircuitBreakEndpointState.CLOSED);
                    break;
                case HALF_OPEN:
                    policy.updateEndpoint(instanceId, CircuitBreakEndpointState.HALF_OPEN);
                    break;
                case OPEN:
                    int waitDurationInOpenState = policy.getWaitDurationInOpenState() <= 0 ? DEFAULT_WAIT_DURATION_IN_OPEN_STATE : policy.getWaitDurationInOpenState();
                    policy.addEndpoint(CircuitBreakEndpoint.open(instanceId, System.currentTimeMillis() + waitDurationInOpenState));
                    break;
                case DISABLED:
                    policy.removeEndpoint(instanceId);
            }
        }

        @Override
        public void close() {
            policy.removeEndpoint(instanceId);
        }
    }
}
