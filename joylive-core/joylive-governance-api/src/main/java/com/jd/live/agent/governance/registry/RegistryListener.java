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

/**
 * Listener for registry events with version tracking and filtering capabilities.
 * <p>
 * Filters events by service and group, and ensures only newer versions are processed.
 * Thread-safe for concurrent event publishing.
 */
@Getter
public class RegistryListener {

    private final String service;

    private final String group;

    private final Consumer<RegistryEvent> consumer;

    private final Object mutex = new Object();

    private long version;

    public RegistryListener(String service, String group, Consumer<RegistryEvent> consumer) {
        this.service = service;
        this.group = group;
        this.consumer = consumer;
    }

    /**
     * Publishes an event if it matches criteria and has newer version.
     * <p>
     * Thread-safe operation that ensures only newer versions are processed.
     *
     * @param event the registry event to process
     */
    public void publish(RegistryEvent event) {
        if (event != null && event.getVersion() > version) {
            synchronized (mutex) {
                if (event.getVersion() > version) {
                    version = event.getVersion();
                    consumer.accept(event);
                }
            }

        }
    }

    /**
     * Checks if listener matches given service and group.
     *
     * @param service      service name to match
     * @param group        group name to match
     * @param defaultGroup default group name for fallback matching
     * @return true if both service and group match
     */
    public boolean match(String service, String group, String defaultGroup) {
        return ServiceId.match(this.service, this.group, service, group, defaultGroup);
    }

}
