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

import com.jd.live.agent.core.inject.annotation.Config;
import com.jd.live.agent.core.inject.annotation.Configurable;
import lombok.Getter;
import lombok.Setter;

/**
 * AgentConfig
 *
 * @since 1.0.0
 */
@Getter
@Setter
@Configurable(prefix = "agent")
public class AgentConfig {

    public static final String COMPONENT_AGENT_CONFIG = "agentConfig";

    @Config("enhance")
    private EnhanceConfig enhanceConfig = new EnhanceConfig();

    @Config("plugin")
    private PluginConfig pluginConfig = new PluginConfig();
}
