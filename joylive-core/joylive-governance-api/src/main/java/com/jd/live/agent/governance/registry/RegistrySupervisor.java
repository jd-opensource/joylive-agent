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
package com.jd.live.agent.governance.registry;

import java.util.List;

/**
 * An interface that defines the method for updating a service group's endpoints in the registry.
 */
public interface RegistrySupervisor extends Registry {

    /**
     * Updates the instances for a specific service group in the registry.
     *
     * @param service   the service
     * @param instances the new list of instances for the service group
     */
    void update(String service, List<ServiceEndpoint> instances);

    /**
     * Checks if the registry is subscribed to a specific service.
     *
     * @param service the service
     * @return true if the registry is subscribed to the service, false otherwise
     */
    boolean isSubscribed(String service);
}
