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
package com.jd.live.agent.governance.subscription.policy;

/**
 * An interface for watching and managing configuration updates.
 */
public interface PolicyWatcher {

    /**
     * A constant representing the live space policy.
     */
    String TYPE_LIVE_POLICY = "live";

    /**
     * A constant representing the service space policy.
     */
    String TYPE_SERVICE_POLICY = "service";

    /**
     * A constant representing the lane space policy.
     */
    String TYPE_LANE_POLICY = "lane";

    /**
     * Adds a listener for configuration updates of the specified type.
     *
     * @param type     The type of configuration space to listen for updates.
     * @param listener The listener to be notified of configuration updates.
     */
    void addListener(String type, PolicyListener listener);

    /**
     * Removes a listener for configuration updates of the specified type.
     *
     * @param type     The type of configuration space to stop listening for updates.
     * @param listener The listener to be removed.
     */
    void removeListener(String type, PolicyListener listener);

}
