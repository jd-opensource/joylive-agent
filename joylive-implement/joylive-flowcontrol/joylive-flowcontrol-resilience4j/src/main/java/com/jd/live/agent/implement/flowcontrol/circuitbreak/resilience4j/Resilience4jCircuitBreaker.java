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
import com.jd.live.agent.governance.invoke.circuitbreak.*;
import com.jd.live.agent.governance.policy.PolicyId;
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

/**
 * Resilience4jCircuitBreaker
 *
 * @since 1.1.0
 */
public class Resilience4jCircuitBreaker extends AbstractCircuitBreaker {

    private static final Logger logger = LoggerFactory.getLogger(Resilience4jCircuitBreaker.class);

    private final io.github.resilience4j.circuitbreaker.CircuitBreaker delegate;

    private final LiveEventConsumer eventConsumer;

    public Resilience4jCircuitBreaker(CircuitBreakPolicy policy, URI uri, CircuitBreaker delegate) {
        super(policy, uri);
        this.delegate = delegate;
        this.eventConsumer = policy.getLevel() != CircuitBreakLevel.INSTANCE
                ? new LiveEventConsumer(this.started, new ServiceStateListener())
                : new LiveEventConsumer(this.started, new InstanceStateListener(uri.getParameter(PolicyId.KEY_SERVICE_ENDPOINT)));
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
     * ServiceStateListener
     *
     * @since 1.1.0
     */
    private class ServiceStateListener implements CircuitBreakerStateListener {

        ServiceStateListener() {
        }

        @Override
        public void onStateChange(CircuitBreakerStateEvent event) {
            if (logger.isDebugEnabled()) {
                logger.debug("[CircuitBreak]State is transitioned from " + event.getFrom() + " to " + event.getTo() + ", uri=" + event.getUri());
            }
            long now = System.currentTimeMillis();
            switch (event.getTo()) {
                case CLOSED:
                    onClose(now);
                    break;
                case HALF_OPEN:
                    onHalfOpen(now);
                    break;
                case OPEN:
                    onOpen(now);
                    break;
                case DISABLED:
                    onDisabled(now);
            }
        }

        protected void onDisabled(long now) {
            // set end time to recovery end time.
            windowRef.set(new CircuitBreakerStateWindow(CircuitBreakerState.DISABLED, now, now + policy.getRecoveryDuration()));
        }

        protected void onOpen(long now) {
            windowRef.set(new CircuitBreakerStateWindow(CircuitBreakerState.OPEN, now, now + policy.getWaitDurationInOpenState()));
        }

        protected void onHalfOpen(long now) {
            windowRef.set(new CircuitBreakerStateWindow(CircuitBreakerState.HALF_OPEN, now, null));
        }

        protected void onClose(long now) {
            windowRef.set(new CircuitBreakerStateWindow(CircuitBreakerState.CLOSED, now, null));
        }
    }

    /**
     * InstanceStateListener
     *
     * @since 1.1.0
     */
    private class InstanceStateListener extends ServiceStateListener implements AutoCloseable {

        private final String instanceId;

        InstanceStateListener(String instanceId) {
            this.instanceId = instanceId;
        }

        @Override
        protected void onClose(long now) {
            super.onClose(now);
            policy.addInspector(instanceId, Resilience4jCircuitBreaker.this);
        }

        @Override
        protected void onDisabled(long now) {
            super.onDisabled(now);
            policy.removeInspector(instanceId, Resilience4jCircuitBreaker.this);
        }

        @Override
        public void close() {
            onDisabled(System.currentTimeMillis());
        }
    }
}
