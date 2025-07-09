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

import com.jd.live.agent.core.util.URI;

/**
 * An interface that provides methods to manage counters associated with service endpoints.
 * Implementations of this interface are responsible for retrieving or creating counters for specific URIs,
 * managing access times, and providing access to the parent {@link CounterManager}.
 */
public interface EndpointCounter {

    /**
     * Returns the {@link Counter} instance associated with the specified URI, creating a new one
     * if it doesn't already exist. This method ensures that a counter is always available for tracking
     * metrics or statistics related to the specified endpoint.
     *
     * @param uri The URI for which to retrieve the counter.
     * @return The {@link Counter} instance, never {@code null}.
     */
    Counter getOrCreateCounter(URI uri);

    /**
     * Sets the access time for the associated resource or object. The access time typically represents
     * the last time the resource was accessed or modified.
     *
     * @param accessTime the access time to set, typically in milliseconds since the epoch (January 1, 1970, 00:00:00 GMT)
     */
    void setAccessTime(long accessTime);

    /**
     * Returns the parent {@link ServiceCounter} instance associated with this endpoint counter.
     * The parent manager is responsible for managing all counters and related resources.
     *
     * @return the parent {@link ServiceCounter} instance
     */
    ServiceCounter getParent();
}



