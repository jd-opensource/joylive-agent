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
package com.jd.live.agent.governance.policy.lane;

import lombok.Getter;
import lombok.Setter;

/**
 * Represents a lane or pathway within a system, which is a logical division of the system.
 */
@Getter
@Setter
public class Lane {

    /**
     * Lane code
     */
    private String code;

    /**
     * Lane name
     */
    private String name;

    /**
     * Whether it is the default lane
     */
    private boolean defaultLane;

    /**
     * Types of fallback strategies for lane redirection
     */
    private FallbackType fallbackType;

    /**
     * When the fallbackType type is CUSTOM, the lane request is redirected based on the lane specified in this field.
     * If there is still no corresponding instance, the request is denied.
     */
    private String fallbackLane;

}
