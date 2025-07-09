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
package com.jd.live.agent.governance.counter;

/**
 * An interface that provides methods to manage and retrieve {@link ServiceCounter} instances
 * associated with specific services and groups. Implementations of this interface are responsible for
 * creating or fetching endpoint counters, which are used to track metrics or statistics
 * for individual services within a group.
 */
public interface CounterManager {

    /**
     * Gets the current flying request instance.
     *
     * @return the flying request object, or null if not available
     */
    FlyingCounter getFlyingCounter();

    /**
     * Retrieves an existing {@link ServiceCounter} associated with the specified service and group,
     * or creates a new one if it does not already exist.
     *
     * @param service the name or identifier of the service
     * @param group   the group to which the service belongs
     * @return the existing or newly created {@link ServiceCounter}, never {@code null}
     */
    ServiceCounter getOrCreateCounter(String service, String group);

    /**
     * Retrieves an existing {@link ServiceCounter} associated with the specified service and group.
     * If no such counter exists, this method returns {@code null}.
     *
     * @param service the name or identifier of the service
     * @param group   the group to which the service belongs
     * @return the existing {@link ServiceCounter}, or {@code null} if it does not exist
     */
    ServiceCounter getCounter(String service, String group);
}


