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

import com.jd.live.agent.core.util.cache.LazyObject;
import com.jd.live.agent.core.util.template.Evaluator;
import com.jd.live.agent.core.util.template.Template;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

/**
 * MQ (Message Queue) configuration class that provides dynamic topic and group name generation
 * based on template expressions. Supports both default configurations and per-topic overrides.
 */
public class MqConfig {

    public static final String DEFAULT_GROUP = "${group}${'_lane_'lane}";

    public static final String DEFAULT_TOPIC = "${topic}${'_lane_'lane}";

    @Getter
    @Setter
    private MqMode liveMode = MqMode.ISOLATION_CLUSTER;

    @Getter
    @Setter
    private MqMode laneMode = MqMode.SHARED;

    @Getter
    @Setter
    private String groupExpression;

    @Getter
    @Setter
    private Map<String, TopicConfig> topics;

    private final LazyObject<Evaluator> groupTemplate = new LazyObject<>(() -> new Template(groupExpression, 128));

    /**
     * Gets the appropriate evaluator for group name generation.
     *
     * @param topic The topic name to check for custom configuration (can be null)
     * @return Evaluator for group name generation, preferring topic-specific configuration if available
     */
    public Evaluator getGroupTemplate(String topic) {
        TopicConfig config = topics == null || topic == null ? null : topics.get(topic);
        Evaluator evaluator = null;
        if (config != null) {
            evaluator = config.getGroupTemplate();
        }
        if (evaluator == null) {
            evaluator = groupTemplate.get();
        }
        return evaluator;
    }

    public boolean isEnabled(String topic) {
        return topic != null && !topic.isEmpty() && topics != null && topics.containsKey(topic);
    }

    /**
     * Gets the MQ mode for lane isolation of specified topic.
     * Falls back to default laneMode if topic not configured.
     *
     * @param topic Target topic name (nullable)
     * @return Configured mode for topic, or default laneMode if not configured
     */
    public MqMode getLaneMode(String topic) {
        TopicConfig config = topics == null || topic == null ? null : topics.get(topic);
        MqMode mode = config == null ? null : config.getLaneMode();
        return mode == null ? laneMode : mode;
    }

    /**
     * Gets the MQ mode for live isolation of specified topic.
     * Falls back to default laneMode if topic not configured.
     *
     * @param topic Target topic name (nullable)
     * @return Configured mode for topic, or default laneMode if not configured
     */
    public MqMode getLiveMode(String topic) {
        TopicConfig config = topics == null || topic == null ? null : topics.get(topic);
        MqMode mode = config == null ? null : config.getLiveMode();
        return mode == null ? laneMode : mode;
    }

}

