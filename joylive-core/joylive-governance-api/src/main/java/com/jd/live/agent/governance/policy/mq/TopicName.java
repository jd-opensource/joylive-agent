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
package com.jd.live.agent.governance.policy.mq;

import com.jd.live.agent.core.extension.annotation.Extensible;

/**
 * The TopicName interface provides methods for converting topic names between source and target,
 * with additional context parameters such as unit and lane for the target topic.
 */
@Extensible("TopicName")
public interface TopicName {

    /**
     * Converts the given topic name to its target equivalent, incorporating additional context parameters.
     *
     * @param topic the original topic name
     * @param unit  the unit context to be included in the target topic name
     * @param lane  the lane context to be included in the target topic name
     * @return the target topic name with the context parameters included
     */
    String getTarget(String topic, String unit, String lane);

    /**
     * Converts the given topic name to its source equivalent.
     *
     * @param topic the original topic name
     * @return the source topic name
     */
    String getSource(String topic);
}
