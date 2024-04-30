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
package com.jd.live.agent.core.instance;

/**
 * Defines the roles that a gateway can assume within an application architecture.
 * This enumeration is used to categorize services based on their function as gateways,
 * distinguishing between frontend gateways, backend gateways, and services that do not
 * serve as gateways.
 */
public enum GatewayRole {

    /**
     * Indicates that the service does not function as a gateway.
     */
    NONE,

    /**
     * Indicates that the service functions as a frontend gateway, typically handling
     * incoming traffic from clients and routing it to appropriate backend services.
     */
    FRONTEND,

    /**
     * Indicates that the service functions as a backend gateway, typically handling
     * requests from frontend services and performing additional processing or routing.
     */
    BACKEND

}

