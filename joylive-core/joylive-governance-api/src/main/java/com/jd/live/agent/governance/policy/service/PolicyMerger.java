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
 * Interface for merging service policies. Implementations of this interface
 * define the behavior for adding, deleting, and updating service policies.
 */
public interface PolicyMerger {

    /**
     * Handles the addition of a new service policy.
     *
     * @param newPolicy The service to be added.
     */
    void onAdd(ServicePolicy newPolicy);

    /**
     * Handles the deletion of an existing service policy.
     *
     * @param oldPolicy The service to be deleted.
     */
    void onDelete(ServicePolicy oldPolicy);

    /**
     * Handles the update of an existing service policy using another policy.
     *
     * @param oldPolicy The service policy to be updated.
     * @param newPolicy The service policy providing the update information.
     */
    void onUpdate(ServicePolicy oldPolicy, ServicePolicy newPolicy);
}
