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

    private String type;

    private String version;

    private String scheme;

    private String host;

    private int port;

    private int weight = 100;

    private Map<String, String> metadata;

    public ServiceInstance() {
    }

    @Builder
    public ServiceInstance(String id,
                           String namespace,
                           String service,
                           String group,
                           String type,
                           String version,
                           String scheme,
                           String host,
                           int port,
                           int weight,
                           Map<String, String> metadata) {
        super(id, namespace, service, group);
        this.type = type;
        this.version = version;
        this.scheme = scheme;
        this.host = host;
        this.port = port;
        this.weight = weight;
        this.metadata = metadata;
    }
}
