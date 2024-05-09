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

package com.jd.live.agent.governance.request;

/**
 * The StickyRequest interface defines the methods that must be implemented by
 * any class that wishes to support "sticky" sessions in a distributed system.
 * Sticky sessions are used to ensure that requests from the same client are
 * consistently routed to the same service instance, typically to maintain session
 * state or to provide a consistent context for stateful interactions.
 */
public interface StickyRequest {

    /**
     * Retrieves the sticky session ID associated with the request, if any.
     * Implementations should return the current session ID that is used to
     * stick the request to a particular service instance. If sticky sessions
     * are not used or the ID is not set, the method should return {@code null}.
     *
     * @return The sticky session ID as a String, or {@code null} if not applicable.
     */
    default String getStickyId() {
        return null;
    }

    /**
     * Sets the sticky session ID for the request. Implementations should use this
     * method to assign a specific session ID that will be used to route subsequent
     * requests to the same service instance. If sticky sessions are not supported,
     * this method may be left empty.
     *
     * @param stickyId The sticky session ID to be set.
     */
    default void setStickyId(String stickyId) {

    }
}
