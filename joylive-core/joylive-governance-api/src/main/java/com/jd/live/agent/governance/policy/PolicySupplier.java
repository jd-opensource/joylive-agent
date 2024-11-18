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
     * Checks if the task associated with the given name has completed successfully.
     *
     * @param name the name of the task to check
     * @return {@code true} if the task with the specified name is done and has not completed exceptionally,
     * otherwise {@code false}
     */
    boolean isDone(String name);
}

