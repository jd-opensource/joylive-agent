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

public class MqConfig {

    public static final String DEFAULT_GROUP = "${group}${'_unit_'unit}${'_lane_'lane}";

    public static final String DEFAULT_TOPIC = "${topic}${'_unit_'unit}${'_lane_'lane}";

    @Getter
    @Setter
    private String group = DEFAULT_GROUP;

    @Getter
    @Setter
    private String topic = DEFAULT_TOPIC;

    private final LazyObject<Evaluator> groupTemplate = new LazyObject<>(() -> new Template(group, 128));

    public Evaluator getGroupTemplate() {
        return groupTemplate.get();
    }

    private final LazyObject<Evaluator> topicTemplate = new LazyObject<>(() -> new Template(topic, 128));

    public Evaluator getTopicTemplate() {
        return topicTemplate.get();
    }

}

