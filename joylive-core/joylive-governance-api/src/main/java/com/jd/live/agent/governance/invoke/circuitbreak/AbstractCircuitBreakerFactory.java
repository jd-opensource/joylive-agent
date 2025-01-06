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

import com.jd.live.agent.core.util.URI;
import com.jd.live.agent.governance.config.RecyclerConfig;
import com.jd.live.agent.governance.invoke.permission.AbstractLicenseeFactory;
import com.jd.live.agent.governance.policy.service.circuitbreak.CircuitBreakPolicy;

/**
 * AbstractCircuitBreakerFactory provides a base implementation for factories that create and manage circuit breakers.
 * It uses a thread-safe map to store and retrieve circuit breakers associated with specific circuit breaker policies.
 * This class is designed to be extended by concrete factory implementations that provide the actual
 * circuit breaker creation logic.
 *
 * @since 1.1.0
 */
public abstract class AbstractCircuitBreakerFactory
        extends AbstractLicenseeFactory<CircuitBreakPolicy, String, CircuitBreaker>
        implements CircuitBreakerFactory {

    @Override
    public CircuitBreaker get(CircuitBreakPolicy policy, URI uri) {
        return get(policy, uri == null ? null : uri.toString(), null, () -> create(policy, uri));
    }

    @Override
    protected RecyclerConfig getConfig() {
        return governanceConfig.getServiceConfig().getCircuitBreaker();
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
    protected abstract CircuitBreaker create(CircuitBreakPolicy policy, URI uri);

}

