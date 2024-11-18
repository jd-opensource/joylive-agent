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

import java.util.List;
import java.util.function.Function;

/**
 * The PolicySupervisor interface extends the PolicySupplier interface to provide additional capabilities
 * for managing and updating governance policies.
 * <p>
 * It allows for the updating of governance policies and retrieving a list of policy subscribers.
 */
public interface PolicySupervisor extends PolicySupplier {

    /**
     * The name of the component for policy supervision.
     */
    String COMPONENT_POLICY_SUPERVISOR = "policySupervisor";

    /**
     * Attempts to update the governance policy.
     *
     * @param expect The expected current governance policy.
     * @param update The new governance policy to update to.
     * @return {@code true} if the update was successful, {@code false} otherwise.
     */
    boolean update(GovernancePolicy expect, GovernancePolicy update);

    /**
     * Attempts to update the governance policy using the provided updater function.
     *
     * @param updater A {@link Function} that takes the current {@link GovernancePolicy} as input
     *                and returns the modified {@link GovernancePolicy}. If {@code null}, the update
     *                operation will not be performed.
     * @return {@code true} if the update operation was successful, {@code false} otherwise. This can
     * include scenarios where the {@code updater} is {@code null}, or if the internal update
     * mechanism (e.g., replacing the old policy with the new one) fails.
     */
    default boolean update(Function<GovernancePolicy, GovernancePolicy> updater) {
        if (updater != null) {
            GovernancePolicy old = getPolicy();
            GovernancePolicy update = updater.apply(old);
            if (update != null) {
                update.cache();
                return update(old, update);
            }
        }
        return false;
    }

    /**
     * Retrieves a list of policy subscribers.
     *
     * @return A list of {@link PolicySubscriber} who are subscribed to policy updates.
     */
    List<PolicySubscriber> getSubscribers();

    /**
     * Waits until the application is ready.
     * This method blocks the current thread until the application signals that it is ready to proceed.
     */
    void waitReady();
}
