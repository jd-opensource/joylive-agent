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
package com.jd.live.agent.governance.registry;

import com.jd.live.agent.governance.util.FrameworkVersion;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

/**
 * Represents an instance of a service in a discovery system.
 */
@Getter
@Setter
public class ServiceInstance extends ServiceId {

    private String id;

    private FrameworkVersion framework;

    private String version;

    private String scheme;

    private String host;

    private int port;

    private int weight = 100;

    private Map<String, String> metadata;

    public ServiceInstance() {
    }

    @Builder
    public ServiceInstance(String namespace,
                           String service,
                           String group,
                           boolean interfaceMode,
                           String id,
                           FrameworkVersion framework,
                           String version,
                           String scheme,
                           String host,
                           int port,
                           int weight,
                           Map<String, String> metadata) {
        super(namespace, service, group, interfaceMode);
        this.id = id;
        this.framework = framework;
        this.version = version;
        this.scheme = scheme;
        this.host = host;
        this.port = port;
        this.weight = weight;
        this.metadata = metadata;
    }

    public String getId() {
        if (id == null) {
            id = getAddress();
        }
        return id;
    }

    public String getAddress() {
        if (port > 0) {
            return host + ":" + port;
        } else {
            return host;
        }
    }

    public String getMetadata(String key) {
        return metadata == null || key == null ? null : metadata.get(key);
    }

    public String getMetadata(String key, String defaultValue) {
        String value = getMetadata(key);
        return value == null || value.isEmpty() ? defaultValue : value;
    }

    public String getSchemeAddress() {
        if (port > 0) {
            if (scheme == null || scheme.isEmpty()) {
                return host + ":" + port;
            }
            return scheme + "://" + host + ":" + port;
        } else if (scheme == null || scheme.isEmpty()) {
            return host;
        }
        return scheme + "://" + host;

    }
}
