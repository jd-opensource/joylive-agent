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
package com.jd.live.agent.core.config;

/**
 * An interface for watching and managing configuration updates.
 */
public interface ConfigWatcher {

    /**
     * A constant representing the type of configuration space for live space.
     */
    String TYPE_LIVE_SPACE = "liveSpace";

    /**
     * A constant representing the type of configuration space for service space.
     */
    String TYPE_SERVICE_SPACE = "serviceSpace";

    /**
     * A constant representing the type of configuration space for lane space.
     */
    String TYPE_LANE_SPACE = "laneSpace";

    /**
     * Adds a listener for configuration updates of the specified type.
     *
     * @param type     The type of configuration space to listen for updates.
     * @param listener The listener to be notified of configuration updates.
     */
    void addListener(String type, ConfigListener listener);

    /**
     * Removes a listener for configuration updates of the specified type.
     *
     * @param type     The type of configuration space to stop listening for updates.
     * @param listener The listener to be removed.
     */
    void removeListener(String type, ConfigListener listener);

}
