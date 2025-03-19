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

@FunctionalInterface
public interface ServiceRegistryFactory {

    /**
     * Retrieves the {@link ServiceRegistry} for the specified service name.
     *
     * @param service the name of the service for which the registry is being retrieved
     * @return the {@link ServiceRegistry} associated with the specified service name
     */
    ServiceRegistry getServiceRegistry(String service);

}
