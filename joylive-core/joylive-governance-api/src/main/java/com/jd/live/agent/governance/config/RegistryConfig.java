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
package com.jd.live.agent.governance.config;

import com.jd.live.agent.core.inject.annotation.Config;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

import static com.jd.live.agent.governance.config.SubscribeMode.AUTO;

@Getter
@Setter
public class RegistryConfig {

    /**
     * The name used to identify the registry configuration component.
     */
    public static final String COMPONENT_REGISTRY_CONFIG = "registryConfig";

    private long heartbeatInterval = 5000L;

    private boolean enabled;

    private boolean registerAppServiceEnabled;

    private SubscribeMode subscribeMode = AUTO;

    private List<RegistryClusterConfig> clusters;

    @Config("host")
    private HostConfig hostConfig = new HostConfig();

    public boolean isRegisterAppServiceEnabled() {
        return registerAppServiceEnabled && enabled;
    }

    public boolean isEmpty() {
        return clusters == null || clusters.isEmpty();
    }
}

