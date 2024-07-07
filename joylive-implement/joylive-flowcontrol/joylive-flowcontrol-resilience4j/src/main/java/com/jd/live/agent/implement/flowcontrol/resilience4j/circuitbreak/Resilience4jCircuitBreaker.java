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

    private String resourceKey;

    public Resilience4jCircuitBreaker(CircuitBreakerPolicy policy,
                                      io.github.resilience4j.circuitbreaker.CircuitBreaker delegate) {
        super(policy);
        this.delegate = delegate;
    }

    /**
     * Try to get a permit return the result
     *
     * @return {@code true} if a permission was acquired and {@code false} otherwise
     */
    @Override
    public boolean acquire() {
        return this.delegate.tryAcquirePermission();
    }

    /**
     * Release the permission
     */
    @Override
    public void release() {
        this.delegate.releasePermission();
    }

    /**
     * Records a successful call. This method must be invoked when a call was
     * successful.
     *
     * @param durationInMs The elapsed time duration of the call
     */
    @Override
    public void onSuccess(long durationInMs) {
        this.delegate.onSuccess(durationInMs, TimeUnit.MILLISECONDS);
    }

    /**
     * Records a failed call. This method must be invoked when a call failed.
     *
     * @param durationInMs The elapsed time duration of the call
     * @param throwable    The throwable which must be recorded
     */
    @Override
    public void onError(long durationInMs, Throwable throwable) {
        this.delegate.onError(durationInMs, TimeUnit.MILLISECONDS, throwable);
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

    /**
     * Get resource key
     *
     * @return Resource key
     */
    @Override
    public String getResourceKey() {
        return resourceKey;
    }

    /**
     * Set resource key
     *
     * @param resourceKey Resource key
     */
    @Override
    public void setResourceKey(String resourceKey) {
        this.resourceKey = resourceKey;
    }

}
