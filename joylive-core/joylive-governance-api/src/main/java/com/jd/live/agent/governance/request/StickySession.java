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

import com.jd.live.agent.governance.policy.service.loadbalance.StickyType;
import lombok.Getter;
import lombok.Setter;

/**
 * Enables sticky session routing for consistent client-to-instance mapping in distributed systems.
 * Useful for maintaining session state or stateful interactions.
 */
public interface StickySession {

    /**
     * Gets the current sticky session ID for request routing.
     *
     * @return The sticky session ID, or null if not set
     */
    default String getStickyId() {
        return null;
    }

    /**
     * Sets the sticky session ID for request routing.
     *
     * @param stickyId The ID to use for sticky routing
     */
    default void setStickyId(String stickyId) {
    }

    /**
     * Gets the type of stickiness applied.
     *
     * @return The sticky routing type
     */
    default StickyType getStickyType() {
        return StickyType.NONE;
    }

    class DefaultStickySession implements StickySession {
        @Getter
        private final StickyType stickyType;
        @Getter
        @Setter
        private String stickyId;

        public DefaultStickySession(StickyType stickyType) {
            this.stickyType = stickyType;
        }

        public DefaultStickySession(StickyType stickyType, String stickyId) {
            this.stickyType = stickyType;
            this.stickyId = stickyId;
        }
    }
}
