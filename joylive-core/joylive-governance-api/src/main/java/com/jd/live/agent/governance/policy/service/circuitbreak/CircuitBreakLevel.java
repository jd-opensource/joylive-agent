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
package com.jd.live.agent.governance.policy.service.circuitbreak;

/**
 * Represents different levels at which circuit breaking can be applied.
 *
 * @since 1.1.0
 */
public enum CircuitBreakLevel {

    /**
     * Circuit breaking at the service level.
     */
    SERVICE,

    /**
     * Circuit breaking at the API level.
     */
    API,

    /**
     * Circuit breaking at the instance level.
     */
    INSTANCE {
        @Override
        public boolean isProtectionSupported() {
            return true;
        }
    };

    public boolean isProtectionSupported() {
        return false;
    }
}