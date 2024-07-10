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

import com.jd.live.agent.core.inject.annotation.Inject;
import com.jd.live.agent.core.util.URI;
import com.jd.live.agent.core.util.time.Timer;
import com.jd.live.agent.governance.config.GovernanceConfig;
import com.jd.live.agent.governance.policy.service.circuitbreak.CircuitBreakerPolicy;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * AbstractCircuitBreakerFactory provides a base implementation for factories that create and manage circuit breakers.
 * It uses a thread-safe map to store and retrieve circuit breakers associated with specific circuit breaker policies.
 * This class is designed to be extended by concrete factory implementations that provide the actual
 * circuit breaker creation logic.
 *
 * @since 1.1.0
 */
public abstract class AbstractCircuitBreakerFactory implements CircuitBreakerFactory {

    /**
     * A thread-safe map to store circuit breakers associated with their respective policies.
     * Key is the string URI of the policy, and the values are atomic references to the circuit breakers.
     */
    private final Map<String, AtomicReference<CircuitBreaker>> circuitBreakers = new ConcurrentHashMap<>();

    @Inject(Timer.COMPONENT_TIMER)
    private Timer timer;

    @Inject(GovernanceConfig.COMPONENT_GOVERNANCE_CONFIG)
    private GovernanceConfig governanceConfig;

    private final AtomicBoolean recycled = new AtomicBoolean(false);

    @Override
    public CircuitBreaker get(CircuitBreakerPolicy policy, URI uri) {
        if (policy == null || uri == null) {
            return null;
        }
        AtomicReference<CircuitBreaker> reference = circuitBreakers.computeIfAbsent(uri.toString(), n -> new AtomicReference<>());
        CircuitBreaker circuitBreaker = reference.get();
        if (circuitBreaker != null && circuitBreaker.getPolicy().getVersion() == policy.getVersion()) {
            return circuitBreaker;
        }
        CircuitBreaker breaker = create(policy, uri);
        while (true) {
            circuitBreaker = reference.get();
            if (circuitBreaker == null || circuitBreaker.getPolicy().getVersion() < policy.getVersion()) {
                if (reference.compareAndSet(circuitBreaker, breaker)) {
                    circuitBreaker = breaker;
                    if (recycled.compareAndSet(false, true)) {
                        addRecycler();
                    }
                    break;
                }
            }
        }
        return circuitBreaker;
    }

    /**
     * Schedules a recurring task to recycle circuit breakers based on their expiration time.
     * This method retrieves the clean interval from the configuration and sets up a delayed task
     * that calls the {@link #recycle()} method and reschedules itself.
     */
    private void addRecycler() {
        long cleanInterval = governanceConfig.getServiceConfig().getCircuitBreaker().getCleanInterval();
        timer.delay("recycle-circuitbreaker", cleanInterval, () -> {
            recycle();
            addRecycler();
        });
    }

    /**
     * Recycles expired circuit breakers. This method checks each circuit breaker to see if it has
     * expired based on the current time and the configured expiration time. If a circuit breaker
     * is not open and has exceeded its expiration time, it is removed from the collection.
     */
    private void recycle() {
        long expireTime = governanceConfig.getServiceConfig().getCircuitBreaker().getExpireTime();
        for (Map.Entry<String, AtomicReference<CircuitBreaker>> entry : circuitBreakers.entrySet()) {
            AtomicReference<CircuitBreaker> reference = entry.getValue();
            CircuitBreaker circuitBreaker = reference.get();
            if (circuitBreaker != null && !circuitBreaker.isOpen() && (System.currentTimeMillis() - circuitBreaker.getLastAcquireTime()) > expireTime) {
                reference = circuitBreakers.remove(entry.getKey());
                if (reference != null) {
                    circuitBreaker = reference.get();
                    if (circuitBreaker != null && (System.currentTimeMillis() - circuitBreaker.getLastAcquireTime()) <= expireTime) {
                        circuitBreakers.putIfAbsent(entry.getKey(), reference);
                    }
                }
            }
        }
    }

    /**
     * Creates a new circuit breaker instance based on the provided circuit breaker policy.
     * This method is abstract and must be implemented by subclasses to provide the specific
     * circuit breaker creation logic.
     *
     * @param policy The circuit breaker policy to be used for creating the circuit breaker.
     * @param uri    The resource uri.
     * @return A new circuit breaker instance that enforces the given policy.
     */
    protected abstract CircuitBreaker create(CircuitBreakerPolicy policy, URI uri);

}

