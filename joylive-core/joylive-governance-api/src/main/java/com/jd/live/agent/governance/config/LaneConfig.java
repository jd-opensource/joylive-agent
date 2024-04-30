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
import lombok.Setter;

/**
 * LaneConfig is a configuration class that holds the keys for identifying specific lanes within a system.
 * It provides constants for the component name and keys used to represent the space ID and lane code.
 */
@Getter
@Setter
public class LaneConfig {

    /**
     * The name used to identify the lane configuration component.
     */
    public static final String COMPONENT_LANE_CONFIG = "laneConfig";

    /**
     * The key used to reference the lane space ID in configurations or headers.
     */
    public static final String KEY_LANE_SPACE_ID = "x-lane-space-id";

    /**
     * The key used to reference the lane code in configurations or headers.
     */
    public static final String KEY_LANE_CODE = "x-lane-code";

    /**
     * The space ID key used for lane configuration.
     */
    private String spaceIdKey = KEY_LANE_SPACE_ID;

    /**
     * The code key used for lane configuration.
     */
    private String codeKey = KEY_LANE_CODE;

}

