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
 * Manages sticky session routing between clients and service instances.
 */
public interface StickyRequest {

    /**
     * Gets or creates a sticky session using the specified routing policy.
     * @param sessionFactory The factory that determines sticky session behavior
     * @return The sticky session instance
     */
    default StickySession getStickySession(StickySessionFactory sessionFactory) {
        return null;
    }

    /**
     * Gets the current sticky session ID used for request routing.
     *
     * @return The sticky session ID, or null if not set
     */
    default String getStickyId() {
        return null;
    }
}
