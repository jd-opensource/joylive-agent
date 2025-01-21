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

import com.jd.live.agent.core.extension.annotation.Extension;
import com.jd.live.agent.core.inject.annotation.Injectable;
import com.jd.live.agent.core.util.URI;
import com.jd.live.agent.governance.invoke.circuitbreak.AbstractCircuitBreakerFactory;
import com.jd.live.agent.governance.invoke.circuitbreak.CircuitBreaker;
import com.jd.live.agent.governance.policy.service.circuitbreak.CircuitBreakPolicy;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.SlidingWindowType;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;

import java.time.Duration;

import static com.jd.live.agent.governance.policy.service.circuitbreak.CircuitBreakPolicy.SLIDING_WINDOW_COUNT;

/**
 * Resilience4jCircuitBreakerFactory
 *
 * @since 1.1.0
 */
@Injectable
@Extension(value = "Resilience4j")
public class Resilience4jCircuitBreakerFactory extends AbstractCircuitBreakerFactory {

    private static final CircuitBreakerRegistry REGISTRY = CircuitBreakerRegistry.ofDefaults();

    @Override
    public CircuitBreaker create(CircuitBreakPolicy policy, URI uri) {
        CircuitBreakerConfig config = getBuilder(policy).build();
        String name = uri.toString();
        REGISTRY.remove(name);
        io.github.resilience4j.circuitbreaker.CircuitBreaker cb = REGISTRY.circuitBreaker(name, config);
        if (policy.isForceOpen()) {
            cb.transitionToForcedOpenState();
        }
        return new Resilience4jCircuitBreaker(policy, uri, cb);
    }

    /**
     * Creates and configures a {@link CircuitBreakerConfig.Builder} based on the provided {@link CircuitBreakPolicy}.
     *
     * @param policy The {@link CircuitBreakPolicy} containing the configuration parameters.
     * @return A configured {@link CircuitBreakerConfig.Builder}.
     */
    private CircuitBreakerConfig.Builder getBuilder(CircuitBreakPolicy policy) {
        // TODO Uniform time unit. waitDurationInOpenState
        CircuitBreakerConfig.Builder result = CircuitBreakerConfig.custom()
                .slidingWindowType(SLIDING_WINDOW_COUNT.equals(policy.getSlidingWindowType()) ? SlidingWindowType.COUNT_BASED : SlidingWindowType.TIME_BASED)
                .slidingWindowSize(policy.getSlidingWindowSize())
                .minimumNumberOfCalls(policy.getMinCallsThreshold())
                .failureRateThreshold(policy.getFailureRateThreshold())
                .slowCallRateThreshold(policy.getSlowCallRateThreshold())
                .slowCallDurationThreshold(Duration.ofMillis(policy.getSlowCallDurationThreshold()))
                .waitDurationInOpenState(Duration.ofSeconds(policy.getWaitDurationInOpenState()))
                .permittedNumberOfCallsInHalfOpenState(policy.getAllowedCallsInHalfOpenState())
                .recordException(e -> true);
        if (policy.getMaxWaitDurationInHalfOpenState() > 0) {
            result.maxWaitDurationInHalfOpenState(Duration.ofMillis(policy.getMaxWaitDurationInHalfOpenState()));
        }
        return result;
    }

}
