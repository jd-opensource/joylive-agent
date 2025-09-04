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

import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;

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
     * Prefix for location-related constants.
     */
    String LABEL_LOCATION_PREFIX = "x-location-";

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

    String LABEL_LIVE_POLICY_ID = LABEL_LIVE_PREFIX + "policy-id";

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

    String LABEL_LOCATION_LIVE_SPACE_ID = LABEL_LOCATION_PREFIX + "live-space-id";

    String LABEL_LOCATION_CELL = LABEL_LOCATION_PREFIX + "cell";

    String LABEL_LOCATION_UNIT = LABEL_LOCATION_PREFIX + "unit";

    /**
     * Constant for the label zone.
     */
    String LABEL_ZONE = LABEL_LIVE_PREFIX + "zone";

    /**
     * Constant for the label region.
     */
    String LABEL_REGION = LABEL_LIVE_PREFIX + "region";

    /**
     * Constant for the label cloud.
     */
    String LABEL_CLOUD = LABEL_LIVE_PREFIX + "cloud";

    /**
     * Constant for the instance ip.
     */
    String LABEL_INSTANCE_IP = LABEL_LIVE_PREFIX + "instance-ip";

    /**
     * Constant for the framework.
     */
    String LABEL_FRAMEWORK = LABEL_LIVE_PREFIX + "framework";

    /**
     * Constant for the live agent version.
     */
    String LABEL_AGENT_VERSION = LABEL_LIVE_PREFIX + "agent-version";

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
     * Key for the timestamp property of the endpoint.
     */
    String LABEL_TIMESTAMP = "timestamp";
    /**
     * Key for the weight property of the endpoint.
     */
    String LABEL_WEIGHT = "weight";
    /**
     * Key for the warmup period property of the endpoint.
     */
    String LABEL_WARMUP = "warmup";

    String LABEL_NAMESPACE = "namespace";

    String LABEL_GROUP = "group";

    String LABEL_STATE = "state";

    /**
     * Default value for labels.
     */
    String DEFAULT_VALUE = "";

    /**
     * Constant for the http exception names header.
     */
    String LABEL_EXCEPTION_NAMES = "x-exception-names";

    /**
     * Constant for the http exception message header.
     */
    String LABEL_EXCEPTION_MESSAGE = "x-exception-message";

    String LABEL_SECURE = "secure";

    String GATEWAY_ROUTE_LIVE_ENABLED = "liveEnabled";

    /**
     * Scheme constant for load balancing ("lb").
     */
    String SCHEME_LB = "lb";

    /**
     * Predicate that tests if a string equals {@link #SCHEME_LB} (case-insensitive).
     */
    Predicate<String> PREDICATE_LB = SCHEME_LB::equalsIgnoreCase;

    String K8S_SERVICE_SUFFIX = ".svc.cluster.local";

    /**
     * The default group name for the service.
     */
    String DEFAULT_GROUP = "default";

    /**
     * Predicate that tests if a string ends with {@link #K8S_SERVICE_SUFFIX} (case-insensitive).
     */
    Predicate<String> PREDICATE_K8S_SERVICE = s -> {
        int len = s == null ? 0 : s.length();
        int suffixLength = K8S_SERVICE_SUFFIX.length();
        len = len - suffixLength;
        if (len <= 0) {
            return false;
        }
        return s.regionMatches(true, len, K8S_SERVICE_SUFFIX, 0, suffixLength);
    };

    /**
     * Extracts the Kubernetes service name
     */
    Function<String, String> K8S_SERVICE_NAME_FUNC = s -> {
        int len = s == null ? 0 : s.length();
        int suffixLength = K8S_SERVICE_SUFFIX.length();
        len = len - suffixLength;
        if (len <= 0 || !s.regionMatches(true, len, K8S_SERVICE_SUFFIX, 0, suffixLength)) {
            return null;
        }
        // skip namespace
        int pos = s.lastIndexOf('.', len - 1);
        return pos < 1 ? null : s.substring(0, pos);
    };

    /**
     * Predicate to check if a string represents the default group (null, empty, or equals DEFAULT_GROUP).
     */
    Predicate<String> DEFAULT_GROUP_PREDICATE = s -> s == null || s.isEmpty() || DEFAULT_GROUP.equalsIgnoreCase(s);

    /**
     * BiPredicate to check if the first string is null, empty, or equals the second string (case-insensitive).
     */
    BiPredicate<String, String> DEFAULT_GROUP_BIPREDICATE = (s, g) -> s == null || s.isEmpty() || s.equalsIgnoreCase(g);

    /**
     * BiPredicate to check if two groups are considered the same, treating default groups as equivalent.
     */
    BiPredicate<String, String> SAME_GROUP_PREDICATE = (source, target) -> {
        if (DEFAULT_GROUP_PREDICATE.test(source)) {
            return DEFAULT_GROUP_PREDICATE.test(target) ? true : false;
        }
        return source.equalsIgnoreCase(target);
    };
}

