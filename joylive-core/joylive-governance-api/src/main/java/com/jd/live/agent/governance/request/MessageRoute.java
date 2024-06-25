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
package com.jd.live.agent.governance.request;

import com.jd.live.agent.core.extension.annotation.Extensible;

/**
 * MessageRoute is an interface for converting topic names between source and target.
 * It provides methods to get the target topic name from a source topic name and vice versa.
 */
@Extensible("MessageRoute")
public interface MessageRoute {

    /**
     * Converts the given topic name to its target equivalent based on the specified unit and lane.
     *
     * @param topic the original topic name.
     * @param unit the unit context for the topic transformation.
     * @param lane the lane context for the topic transformation.
     * @return the target topic name after applying the unit and lane transformations.
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

