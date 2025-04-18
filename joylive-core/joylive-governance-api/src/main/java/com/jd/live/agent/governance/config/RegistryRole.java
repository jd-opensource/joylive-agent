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
package com.jd.live.agent.governance.config;

import lombok.Getter;

/**
 * Defines the role types in a registry system with priority ordering.
 */
@Getter
public enum RegistryRole {

    /**
     * Primary role with highest priority (order=2).
     */
    PRIMARY(0),

    /**
     * System-level role with medium priority (order=1).
     */
    SYSTEM(1),

    /**
     * Secondary role with lowest priority (order=0).
     */
    SECONDARY(2);

    RegistryRole(int order) {
        this.order = order;
    }

    final int order;
}
