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
package com.jd.live.agent.core.service;

import java.util.List;
import java.util.function.Consumer;

/**
 * Defines an interface for supervising services within a system.
 * The {@code ServiceSupervisor} is responsible for managing and providing information
 * about agent services that are currently active or available within the system.
 */
public interface ServiceSupervisor {

    String COMPONENT_SERVICE_SUPERVISOR = "serviceSupervisor";

    /**
     * Retrieves a list of {@code AgentService} instances that are currently managed
     * or supervised by this supervisor.
     *
     * @return A list of {@code AgentService} instances representing the services
     * currently under supervision. This list may be empty if no services
     * are currently being supervised.
     */
    List<AgentService> getServices();

    /**
     * Provides a way to access and process the available agent services.
     *
     * @param consumer A callback function that will be invoked for each agent service, passing the service instance as a parameter.
     */
    default void service(Consumer<AgentService> consumer) {
        if (consumer != null) {
            List<AgentService> services = getServices();
            if (services != null && !services.isEmpty()) {
                for (AgentService service : services) {
                    consumer.accept(service);
                }
            }
        }
    }
}

