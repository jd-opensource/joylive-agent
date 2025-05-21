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
public class RegistryDeltaEvent extends RegistryEvent {

    private final EventType type;

    public RegistryDeltaEvent(String service, List<ServiceEndpoint> instances) {
        this(VERSION.incrementAndGet(), service, null, instances, null, EventType.FULL);
    }

    public RegistryDeltaEvent(String service, String group, List<ServiceEndpoint> instances) {
        this(VERSION.incrementAndGet(), service, group, instances, null, EventType.FULL);
    }

    public RegistryDeltaEvent(String service, String group, List<ServiceEndpoint> instances, String defaultGroup) {
        this(VERSION.incrementAndGet(), service, group, instances, defaultGroup, EventType.FULL);
    }

    public RegistryDeltaEvent(String service, String group, List<ServiceEndpoint> instances, EventType type) {
        this(VERSION.incrementAndGet(), service, group, instances, null, type);
    }

    public RegistryDeltaEvent(String service, String group, List<ServiceEndpoint> instances, String defaultGroup, EventType type) {
        this(VERSION.incrementAndGet(), service, group, instances, defaultGroup, type);
    }

    public RegistryDeltaEvent(long version, String service, String group, List<ServiceEndpoint> instances, String defaultGroup, EventType type) {
        super(version, service, group, instances, defaultGroup);
        this.type = type;
    }

    @Override
    public boolean isFull() {
        return EventType.FULL == type;
    }

    public enum EventType {

        FULL,

        ADD,

        UPDATE,

        REMOVE,

    }
}
