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
package com.jd.live.agent.governance.policy.service.live;

/**
 * Defines the policy for unit handling within a distributed system or application context.
 *
 * @author Zhiguo.Chen
 * @since 1.0.0
 */
public enum UnitPolicy {

    /**
     * Indicates that no specific unit policy is applied.
     */
    NONE,

    /**
     * Traffic is routed to the central unit
     */
    CENTER,

    /**
     * Unitization strategy, traffic closed-loop within a specified unit.
     */
    UNIT,

    /**
     * Indicates a preference for local units when performing operations.
     */
    PREFER_LOCAL_UNIT
}
