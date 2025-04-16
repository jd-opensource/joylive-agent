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

import java.util.function.Consumer;

@Getter
public class RegistryListener {

    private final String service;

    private final String group;

    private final Consumer<RegistryEvent> consumer;

    public RegistryListener(String service, String group, Consumer<RegistryEvent> consumer) {
        this.service = service;
        this.group = group;
        this.consumer = consumer;
    }

    public synchronized void publish(RegistryEvent event) {
        if (event != null) {
            consumer.accept(event);
        }
    }

    public boolean match(String service, String group, String defaultGroup) {
        return isService(service) && isGroup(group, defaultGroup);
    }

    public boolean isGroup(String group, String defaultGroup) {
        if (this.group == null || this.group.isEmpty()) {
            return group == null
                    || group.isEmpty()
                    || defaultGroup != null && !defaultGroup.isEmpty() && defaultGroup.equalsIgnoreCase(group);
        }
        return this.group.equals(group);
    }

    public boolean isService(String service) {
        return this.service.equalsIgnoreCase(service);
    }

}
