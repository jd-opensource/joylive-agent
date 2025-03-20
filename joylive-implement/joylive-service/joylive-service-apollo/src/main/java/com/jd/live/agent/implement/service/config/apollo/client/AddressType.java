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
package com.jd.live.agent.implement.service.config.apollo.client;

/**
 * Represents the type of address used in the system.
 * This enumeration defines the possible address types, such as meta server and config server.
 */
public enum AddressType {

    /**
     * Represents a Meta Server address.
     * Meta Server is typically used for metadata management and coordination.
     */
    META_SERVER,

    /**
     * Represents a Config Server address.
     * Config Server is typically used for configuration management and dynamic updates.
     */
    CONFIG_SERVER,

}

