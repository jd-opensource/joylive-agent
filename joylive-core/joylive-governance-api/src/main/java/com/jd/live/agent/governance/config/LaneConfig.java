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

import java.util.Set;

/**
 * LaneConfig is a configuration class that holds the keys for identifying specific lanes within a system.
 */
@Getter
@Setter
public class LaneConfig {

    private boolean fallbackLocationIfNoSpace = true;

    private boolean modifyMQGroupEnabled = true;

    private Set<String> topics;

    public boolean withTopic(String topic) {
        return topic != null && topics != null && topics.contains(topic);
    }

}

