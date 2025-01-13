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
package com.jd.live.agent.core.plugin;

/**
 * Represents the various statuses a plugin can be in during its lifecycle.
 */
public enum PluginStatus {

    /**
     * The plugin has been created but not yet loaded
     */
    CREATED,

    /**
     * The plugin's definitions have been loaded
     */
    LOADED,

    /**
     * The plugin has been successfully installed and is operational
     */
    SUCCESS,

    /**
     * The plugin has failed to load properly
     */
    FAILED
}
