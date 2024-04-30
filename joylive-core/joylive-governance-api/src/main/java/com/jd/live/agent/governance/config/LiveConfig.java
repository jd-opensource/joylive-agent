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
 * LiveConfig is a configuration class that holds the keys for managing live settings within a system.
 * It provides constants for various live configuration properties such as rule ID, space ID, and live variable.
 */
@Getter
@Setter
public class LiveConfig {

    /**
     * The name used to identify the live configuration component.
     */
    public static final String COMPONENT_LIVE_CONFIG = "liveConfig";

    /**
     * The key used to reference the live rule ID in configurations or headers.
     */
    public static final String KEY_LIVE_RULE_ID = "x-live-rule-id";

    /**
     * The key used to reference the live space ID in configurations or headers.
     */
    public static final String KEY_LIVE_SPACE_ID = "x-live-space-id";

    /**
     * The key used to reference the live variable, often a unique identifier, in configurations or headers.
     */
    public static final String KEY_LIVE_VARIABLE = "x-live-uid";

    /**
     * A common prefix for live configuration keys to maintain consistency in naming.
     */
    public static final String KEY_LIVE_PREFIX = "x-live-";

    /**
     * The space ID key used for live configuration.
     */
    private String spaceIdKey = KEY_LIVE_SPACE_ID;

    /**
     * The variable key used for live configuration, which may represent a unique identifier for live sessions.
     */
    private String variableKey = KEY_LIVE_VARIABLE;

    /**
     * The rule ID key used for live configuration, which may determine the behavior of live sessions.
     */
    private String ruleIdKey = KEY_LIVE_RULE_ID;

}

