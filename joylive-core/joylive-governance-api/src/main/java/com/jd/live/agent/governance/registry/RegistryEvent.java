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

import lombok.*;

import java.io.Serializable;

/**
 * Represents an event in the service registry.
 * This event contains the type of event and the service instance involved.
 */
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RegistryEvent implements Serializable {

    private EventType type;

    private ServiceInstance instance;

    public static RegistryEvent ofRegister(ServiceInstance instance) {
        return new RegistryEvent(EventType.REGISTER, instance);
    }

    public static RegistryEvent ofUnregister(ServiceInstance instance) {
        return new RegistryEvent(EventType.UNREGISTER, instance);
    }

    public static RegistryEvent ofHeartbeat(ServiceInstance instance) {
        return new RegistryEvent(EventType.HEARTBEAT, instance);
    }

    /**
     * Enum representing the type of registry event.
     */
    public enum EventType {
        /**
         * Indicates a register event.
         */
        REGISTER,

        /**
         * Indicates a unregister event.
         */
        UNREGISTER,

        /**
         * Indicates a heartbeat event.
         */
        HEARTBEAT
    }

}
