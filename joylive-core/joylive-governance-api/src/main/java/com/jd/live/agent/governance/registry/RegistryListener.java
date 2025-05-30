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

    private final ServiceId serviceId;

    private final Consumer<RegistryEvent> consumer;

    private final Object mutex = new Object();

    private long version;

    public RegistryListener(ServiceId serviceId, Consumer<RegistryEvent> consumer) {
        this.serviceId = serviceId;
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
     * Checks if this listener's service matches the target service ID.
     * Delegates matching logic to {@link ServiceId#match(ServiceId, String)}.
     *
     * @param target the service ID to match against (may be null)
     * @param defaultGroup fallback group used when matching (may be null)
     * @return true if services match according to ServiceId matching rules
     */
    public boolean match(ServiceId target, String defaultGroup) {
        return serviceId.match(target, defaultGroup);
    }

}
