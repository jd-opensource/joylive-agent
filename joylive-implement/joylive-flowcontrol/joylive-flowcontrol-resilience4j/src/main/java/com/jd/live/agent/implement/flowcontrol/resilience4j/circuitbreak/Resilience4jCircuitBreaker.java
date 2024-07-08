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

import com.jd.live.agent.core.util.URI;
import com.jd.live.agent.governance.invoke.circuitbreak.AbstractCircuitBreaker;
import com.jd.live.agent.governance.invoke.circuitbreak.CircuitBreakerStateListener;
import com.jd.live.agent.governance.policy.service.circuitbreaker.CircuitBreakerPolicy;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.core.EventConsumer;

import java.util.concurrent.TimeUnit;

/**
 * Resilience4jCircuitBreaker
 *
 * @since 1.1.0
 */
public class Resilience4jCircuitBreaker extends AbstractCircuitBreaker {

    private final io.github.resilience4j.circuitbreaker.CircuitBreaker delegate;

    public Resilience4jCircuitBreaker(CircuitBreakerPolicy policy, URI uri, CircuitBreaker delegate) {
        super(policy, uri);
        this.delegate = delegate;
    }

    @Override
    public boolean acquire() {
        return this.delegate.tryAcquirePermission();
    }

    @Override
    public void release() {
        this.delegate.releasePermission();
    }

    @Override
    public void onSuccess(long durationInMs) {
        this.delegate.onSuccess(durationInMs, TimeUnit.MILLISECONDS);
    }

    @Override
    public void onError(long durationInMs, Throwable throwable) {
        this.delegate.onError(durationInMs, TimeUnit.MILLISECONDS, throwable);
    }

    @Override
    public void registerListener(CircuitBreakerStateListener listener) {
        if (listener instanceof EventConsumer) {
            this.delegate.getEventPublisher().onStateTransition((EventConsumer) listener);
        }
    }

}
