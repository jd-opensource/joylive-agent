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
package com.jd.live.agent.core.service;

import com.jd.live.agent.core.config.ConfigWatcher;

/**
 * An interface for a configuration service that extends the AgentService interface and implements the ConfigWatcher interface.
 */
public interface ConfigService extends AgentService, ConfigWatcher {

    /**
     * Returns the type of the configuration service.
     *
     * @return The type of the configuration service.
     */
    String getType();
}

