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
package com.jd.live.agent.core;

/**
 * A collection of constant values used as labels or keys in configurations or headers.
 * These constants are typically prefixed to indicate their context (e.g., live, lane, service).
 */
public interface Constants {

    /**
     * Prefix for live-related constants.
     */
    String LABEL_LIVE_PREFIX = "x-live-";

    /**
     * Constant for the label application.
     */
    String LABEL_APPLICATION = LABEL_LIVE_PREFIX + "application";

    /**
     * Constant for the label cluster.
     */
    String LABEL_CLUSTER = LABEL_LIVE_PREFIX + "cluster";

    /**
     * Constant for the instance ID.
     */
    String LABEL_INSTANCE_ID = LABEL_LIVE_PREFIX + "instance-id";

    /**
     * The key used to reference the live variable, often a unique identifier, in configurations or headers.
     */
    String LABEL_VARIABLE = LABEL_LIVE_PREFIX + "uid";

    /**
     * Constant for the label cell.
     */
    String LABEL_CELL = LABEL_LIVE_PREFIX + "cell";

    /**
     * Constant for the label unit.
     */
    String LABEL_UNIT = LABEL_LIVE_PREFIX + "unit";

    /**
     * Constant for the label unit rule ID.
     */
    String LABEL_RULE_ID = LABEL_LIVE_PREFIX + "rule-id";

    /**
     * Constant for the label live space ID.
     */
    String LABEL_LIVE_SPACE_ID = LABEL_LIVE_PREFIX + "space-id";

    /**
     * Constant for the label zone.
     */
    String LABEL_ZONE = LABEL_LIVE_PREFIX + "zone";

    /**
     * Constant for the label region.
     */
    String LABEL_REGION = LABEL_LIVE_PREFIX + "region";

    /**
     * Prefix for lane-related constants.
     */
    String LABEL_LANE_PREFIX = "x-lane-";

    /**
     * Constant for the label lane.
     */
    String LABEL_LANE = LABEL_LANE_PREFIX + "code";

    /**
     * Constant for the label lane space ID.
     */
    String LABEL_LANE_SPACE_ID = LABEL_LANE_PREFIX + "space-id";

    /**
     * Prefix for service-related constants.
     */
    String LABEL_SERVICE_PREFIX = "x-service-";

    /**
     * Constant for the label group.
     */
    String LABEL_SERVICE_GROUP = LABEL_SERVICE_PREFIX + "group";

    /**
     * Constant for the service namespace.
     */
    String LABEL_SERVICE_NAMESPACE = LABEL_SERVICE_PREFIX + "namespace";

    /**
     * Constant for the service ID.
     */
    String LABEL_SERVICE_ID = LABEL_SERVICE_PREFIX + "id";

    /**
     * Constant for the service consumer.
     */
    String LABEL_SERVICE_CONSUMER = LABEL_SERVICE_PREFIX + "consumer";

    /**
     * Default value for labels.
     */
    String DEFAULT_VALUE = "";
}

