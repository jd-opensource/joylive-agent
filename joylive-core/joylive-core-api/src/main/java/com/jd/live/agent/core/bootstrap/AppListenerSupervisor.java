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
package com.jd.live.agent.core.bootstrap;

/**
 * Supervisor for managing application listeners.
 */
public interface AppListenerSupervisor extends AppListener {

    /**
     * Adds a listener to be supervised.
     *
     * @param listener the listener to add
     */
    void add(AppListener listener);

    /**
     * Adds a listener to the beginning of the listener chain.
     *
     * @param listener the listener to add
     */
    void addFirst(AppListener listener);

    /**
     * Removes a listener from supervision.
     *
     * @param listener the listener to remove
     */
    void remove(AppListener listener);

}
