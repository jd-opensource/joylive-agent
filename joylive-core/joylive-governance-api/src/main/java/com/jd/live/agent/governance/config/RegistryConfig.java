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

import lombok.Getter;
import lombok.Setter;

import java.net.URI;
import java.util.List;
import java.util.Map;

@Getter
@Setter
public class RegistryConfig {

    /**
     * The name used to identify the registry configuration component.
     */
    public static final String COMPONENT_REGISTRY_CONFIG = "registryConfig";

    private long heartbeatInterval = 5000L;

    private boolean enabled;

    private List<RegistryClusterConfig> clusters;

    private boolean hostServiceEnabled = true;

    private Map<String, String> hostServices;

    public String getService(String scheme, String host) {
        if (host == null
                || !hostServiceEnabled
                || "lb".equalsIgnoreCase(scheme)
                || hostServices == null
                || hostServices.isEmpty()) {
            return host;
        }
        return hostServices.get(host);
    }

    public String getService(URI uri) {
        return uri == null ? null : getService(uri.getScheme(), uri.getHost());
    }

}

