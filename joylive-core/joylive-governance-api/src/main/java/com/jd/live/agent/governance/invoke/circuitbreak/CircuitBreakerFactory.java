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

import com.jd.live.agent.core.extension.annotation.Extensible;
import com.jd.live.agent.governance.invoke.ratelimit.RateLimiter;
import com.jd.live.agent.governance.policy.service.circuitbreaker.CircuitBreakerPolicy;
import com.jd.live.agent.governance.policy.service.limit.RateLimitPolicy;

/**
 * A factory interface for creating instances of {@link CircuitBreaker}.
 * Implementations of this interface define how circuit breakers are
 * instantiated based on a provided {@link CircuitBreakerPolicy}.
 *
 * @since 1.1.0
 */
@Extensible("CircuitBreakerFactory")
public interface CircuitBreakerFactory {

    /**
     * Retrieves a new instance of a {@link CircuitBreaker} based on the provided
     * circuit breaker policy.
     *
     * @param policy the policy that defines the circuit breaker rules.
     * @return a new instance of a circuit breaker configured according to the policy.
     */
    CircuitBreaker get(CircuitBreakerPolicy policy);

}
