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

import lombok.Getter;

import java.util.List;

@Getter
public class RegistryEvent {

    private final String service;

    private final String group;

    private final List<ServiceEndpoint> instances;

    private final String defaultGroup;

    public RegistryEvent(String service, List<ServiceEndpoint> instances) {
        this(service, null, instances, null);
    }

    public RegistryEvent(String service, String group, List<ServiceEndpoint> instances, String defaultGroup) {
        this.service = service;
        this.group = group;
        this.instances = instances;
        this.defaultGroup = defaultGroup;
    }

    public boolean isEmpty() {
        return instances == null || instances.isEmpty();
    }

    public int size() {
        return instances == null ? 0 : instances.size();
    }
}
