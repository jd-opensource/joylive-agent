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

import com.jd.live.agent.governance.policy.service.circuitbreaker.CircuitBreakerPolicy;
import com.jd.live.agent.governance.policy.service.limit.RateLimitPolicy;
import com.jd.live.agent.governance.policy.service.limit.SlidingWindow;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
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
     * The keys are the policy IDs, and the values are atomic references to the circuit breakers.
     */
    protected final Map<Long, AtomicReference<CircuitBreaker>> circuitBreakers = new ConcurrentHashMap<>();

    /**
     * Retrieves a circuit breaker for the given circuit breaker policy. If a circuit breaker for the policy
     * already exists and its version is greater than or equal to the policy version, it is returned.
     * Otherwise, a new circuit breaker is created using the {@link #create(CircuitBreakerPolicy)} method.
     *
     * @param policy The circuit breaker policy for which to retrieve or create a circuit breaker.
     * @return A circuit breaker that corresponds to the given policy, or null if the policy is null.
     */
    @Override
    public CircuitBreaker get(CircuitBreakerPolicy policy) {
        if (policy == null) {
            return null;
        }
        AtomicReference<CircuitBreaker> reference = circuitBreakers.computeIfAbsent(policy.getId(), n -> new AtomicReference<>());
        CircuitBreaker circuitBreaker = reference.get();
        if (circuitBreaker != null && circuitBreaker.getPolicy().getVersion() == policy.getVersion()) {
            return circuitBreaker;
        }
        CircuitBreaker breaker = create(policy);
        while (true) {
            circuitBreaker = reference.get();
            if (circuitBreaker == null || circuitBreaker.getPolicy().getVersion() != policy.getVersion()) {
                if (reference.compareAndSet(circuitBreaker, breaker)) {
                    circuitBreaker = breaker;
                    break;
                }
            }
        }
        return circuitBreaker;
    }

    /**
     * Creates a new circuit breaker instance based on the provided circuit breaker policy.
     * This method is abstract and must be implemented by subclasses to provide the specific
     * circuit breaker creation logic.
     *
     * @param policy The circuit breaker policy to be used for creating the circuit breaker.
     * @return A new circuit breaker instance that enforces the given policy.
     */
    protected abstract CircuitBreaker create(CircuitBreakerPolicy policy);
    
}

