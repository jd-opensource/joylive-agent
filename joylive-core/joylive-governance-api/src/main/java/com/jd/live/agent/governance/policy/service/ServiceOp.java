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
 * Utility class for accessing protected methods in Service class.
 * Provides static methods to handle service operations like delete, update and add.
 */
public class ServiceOp {

    /**
     * Deletes a policy model from service. Returns true if other models still exist.
     *
     * @param service The target service
     * @param merger The policy merger to handle merging
     * @param owner The policy model to delete
     * @return True if service still has other policy models, false otherwise
     */
    public static boolean onDelete(Service service, PolicyMerger merger, String owner) {
        return service.onDelete(merger, owner);
    }

    /**
     * Updates a service with changes from a policy model.
     *
     * @param oldService The service to be updated
     * @param newService The newer to apply
     * @param merger     The policy merger to handle merging
     * @param owner      The policy model identifier
     * @return The updated service
     */
    public static Service onUpdate(Service oldService, Service newService, PolicyMerger merger, String owner) {
        oldService.onUpdate(newService, merger, owner);
        return oldService;
    }

    /**
     * Adds a new policy model to the service.
     *
     * @param service The target service
     * @param merger The policy merger to handle merging
     * @param owner The policy model identifier to add
     * @return The added service
     */
    public static Service onAdd(Service service, PolicyMerger merger, String owner) {
        service.onAdd(merger, owner);
        return service;
    }
}

