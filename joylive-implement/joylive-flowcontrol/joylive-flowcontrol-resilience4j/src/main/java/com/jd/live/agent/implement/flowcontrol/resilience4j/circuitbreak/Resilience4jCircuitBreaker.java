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

import com.jd.live.agent.governance.invoke.circuitbreak.AbstractCircuitBreaker;
import com.jd.live.agent.governance.invoke.circuitbreak.CircuitBreakerStateListener;
import com.jd.live.agent.governance.policy.service.circuitbreaker.CircuitBreakerPolicy;
import io.github.resilience4j.core.EventConsumer;

import java.util.concurrent.TimeUnit;

/**
 * Resilience4jCircuitBreaker
 *
 * @since 1.1.0
 */
public class Resilience4jCircuitBreaker extends AbstractCircuitBreaker {

    private final io.github.resilience4j.circuitbreaker.CircuitBreaker delegate;

    public Resilience4jCircuitBreaker(CircuitBreakerPolicy policy,
                                      io.github.resilience4j.circuitbreaker.CircuitBreaker delegate) {
        super(policy);
        this.delegate = delegate;
    }

    /**
     * Try to get a permit return the result
     *
     * @return permission
     */
    @Override
    public boolean acquire() {
        return this.delegate.tryAcquirePermission();
    }

    /**
     * This method must be invoked when a call returned a result
     * and the result predicate should decide if the call was successful or not.
     *
     * @param duration     The elapsed time duration of the call
     * @param durationUnit The duration unit
     * @param result       The result of the protected function
     */
    @Override
    public void onResult(long duration, TimeUnit durationUnit, Object result) {
        this.delegate.onResult(duration, durationUnit, result);
    }

    /**
     * Records a successful call. This method must be invoked when a call was
     * successful.
     *
     * @param duration     The elapsed time duration of the call
     * @param durationUnit The duration unit
     */
    @Override
    public void onSuccess(long duration, TimeUnit durationUnit) {
        this.delegate.onSuccess(duration, durationUnit);
    }

    /**
     * Records a failed call. This method must be invoked when a call failed.
     *
     * @param duration     The elapsed time duration of the call
     * @param durationUnit The duration unit
     * @param throwable    The throwable which must be recorded
     */
    @Override
    public void onError(long duration, TimeUnit durationUnit, Throwable throwable) {
        this.delegate.onError(duration, durationUnit, throwable);
    }

    /**
     * Register a listener to watch state change event.
     *
     * @param listener State change listener
     */
    @Override
    public void registerListener(CircuitBreakerStateListener listener) {
        if (listener instanceof EventConsumer) {
            this.delegate.getEventPublisher().onStateTransition((EventConsumer) listener);
        }
    }
}
