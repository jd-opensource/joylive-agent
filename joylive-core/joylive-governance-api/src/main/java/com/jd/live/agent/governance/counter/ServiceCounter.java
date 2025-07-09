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

import com.jd.live.agent.governance.instance.Endpoint;

import java.util.List;

/**
 * An interface that provides methods to manage and retrieve {@link EndpointCounter} instances
 * associated with specific endpoints. Implementations of this interface are responsible for
 * creating or fetching URL counters, which are used to track metrics or statistics
 * for individual URLs within an endpoint.
 */
public interface ServiceCounter {

    /**
     * Retrieves an existing {@link EndpointCounter} associated with the specified endpoint,
     * or creates a new one if it does not already exist.
     *
     * @param id the identifier of the endpoint associated with the URL counter
     * @return the existing or newly created {@link EndpointCounter}, never {@code null}
     */
    EndpointCounter getOrCreateCounter(String id);

    /**
     * Schedules a task to clean up counters for endpoints that are no longer in use, using the provided list of
     * current endpoints. The task will not be scheduled if one is already running.
     *
     * @param endpoints The list of current endpoints for the service.
     */
    void tryClean(List<? extends Endpoint> endpoints);
}

