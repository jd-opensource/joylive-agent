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
package com.jd.live.agent.governance.policy;

import com.jd.live.agent.governance.counter.CounterManager;

import java.util.concurrent.CompletableFuture;

/**
 * Represents a supplier of governance policies.
 * <p>
 * Implementations of this interface are responsible for providing instances of {@link GovernancePolicy}.
 * Additionally, they support subscribing to policy updates based on policy name and type.
 * </p>
 *
 * @author Zhiguo.Chen
 * @since 1.0.0
 */
public interface PolicySupplier {

    /**
     * Constant identifier for components implementing this interface.
     */
    String COMPONENT_POLICY_SUPPLIER = "policySupplier";

    /**
     * Retrieves the current governance policy.
     *
     * @return An instance of {@link GovernancePolicy} representing the current governance policy.
     */
    GovernancePolicy getPolicy();

    /**
     * Subscribes a specific service policy based on its name.
     *
     * @param service The service name of the policy to subscribe to.
     * @return A {@link CompletableFuture} that completes when the subscription is successful.
     */
    default CompletableFuture<Void> subscribe(String service) {
        return subscribe(null, service);
    }

    /**
     * Subscribes a specific service policy based on its name.
     *
     * @param namespace The namespace of the service.
     * @param service   The service name of the policy to subscribe to.
     * @return A {@link CompletableFuture} that completes when the subscription is successful.
     */
    CompletableFuture<Void> subscribe(String namespace, String service);

    /**
     * Checks if the specified service policy is ready in the default namespace.
     *
     * @param service the name of the service to check (must not be {@code null} or empty)
     * @return {@code true} if the service is ready in the default namespace,
     * {@code false} otherwise
     */
    default boolean isReady(String service) {
        return isReady(null, service);
    }

    /**
     * Checks if the specified service policy in the given namespace is ready for operation.
     *
     * @param namespace the namespace containing the service (may be {@code null} for default namespace)
     * @param service   the name of the service to check (must not be {@code null} or empty)
     * @return {@code true} if the service exists and is ready in the specified namespace,
     * {@code false} otherwise
     */
    boolean isReady(String namespace, String service);

    CounterManager getCounterManager();
}

