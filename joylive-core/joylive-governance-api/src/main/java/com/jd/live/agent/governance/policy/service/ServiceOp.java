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
 * Utility class providing static methods to operate on {@code Service} objects.
 * This class defines operations such as delete, update, and add that can be performed on services.
 */
public class ServiceOp {

    /**
     * Invokes the onDelete method on the provided {@code Service} object.
     *
     * @param service The {@code Service} object on which the delete operation is to be performed.
     * @param merger  The {@code PolicyMerger} used to handle the merging of policies during the delete operation.
     * @param owner   The owner identifier as a {@code String} that is associated with the service.
     * @return A boolean value indicating the success or failure of the delete operation.
     */
    public static boolean onDelete(Service service, PolicyMerger merger, String owner) {
        return service.onDelete(merger, owner);
    }

    /**
     * Invokes the onUpdate method on the provided {@code Service} object with the given update parameters.
     *
     * @param service The {@code Service} object on which the update operation is to be performed.
     * @param update  The {@code Service} object that contains the update details.
     * @param merger  The {@code PolicyMerger} used to handle the merging of policies during the update operation.
     * @param owner   The owner identifier as a {@code String} that is associated with the service being updated.
     */
    public static void onUpdate(Service service, Service update, PolicyMerger merger, String owner) {
        service.onUpdate(update, merger, owner);
    }

    /**
     * Invokes the onAdd method on the provided {@code Service} object.
     *
     * @param service The {@code Service} object on which the add operation is to be performed.
     * @param merger  The {@code PolicyMerger} used to handle the merging of policies during the add operation.
     * @param owner   The owner identifier as a {@code String} that is associated with the service being added.
     */
    public static void onAdd(Service service, PolicyMerger merger, String owner) {
        service.onAdd(merger, owner);
    }
}

