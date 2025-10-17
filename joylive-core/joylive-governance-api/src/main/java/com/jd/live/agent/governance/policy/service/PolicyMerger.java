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
package com.jd.live.agent.governance.policy.service;

/**
 * Merges different policy models by handling policy additions, deletions and updates.
 * Defines the core operations for policy model management.
 */
public interface PolicyMerger {

    /**
     * Handles new policy addition
     * @param newPolicy Policy to add
     */
    void onAdd(ServicePolicy newPolicy);

    /**
     * Handles policy deletion
     * @param oldPolicy Policy to delete
     */
    void onDelete(ServicePolicy oldPolicy);

    /**
     * Updates existing policy with new policy data
     * @param oldPolicy Policy to update
     * @param newPolicy Policy containing new data
     */
    void onUpdate(ServicePolicy oldPolicy, ServicePolicy newPolicy);

    /**
     * Handles service addition
     *
     * @param service Service to add
     */
    default void onAdd(Service service) {

    }

    /**
     * Handles service deletion
     *
     * @param service Service to delete
     */
    default void onDelete(Service service) {

    }

    /**
     * Updates existing service with new service data
     *
     * @param oldService Service to update
     * @param newService Service containing new data
     */
    default void onUpdate(Service oldService, Service newService) {

    }
}
