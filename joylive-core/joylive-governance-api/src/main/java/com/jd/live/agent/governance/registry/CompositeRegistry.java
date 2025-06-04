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

/**
 * Extends {@link Registry} with composite registry capabilities.
 * Supports managing multiple registry services with different implementations.
 */
public interface CompositeRegistry extends Registry {

    /**
     * Set the system registry service .
     *
     * @param registryService the service implementation to add
     */
    void addSystemRegistry(RegistryService registryService);

    void removeSystemRegistry(RegistryService registryService);

    /**
     * Set the system registry service .
     *
     * @param service         the service name
     * @param registryService the service implementation to add
     */
    void addSystemRegistry(String service, RegistryService registryService);
}
