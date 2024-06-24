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
package com.jd.live.agent.governance.policy.mq.suffix;

import com.jd.live.agent.core.extension.annotation.Extension;
import com.jd.live.agent.governance.policy.mq.TopicName;

@Extension("suffix")
public class SuffixTopicName implements TopicName {

    private static final String LANE = "-lane-";

    private static final String UNIT = "-unit-";

    @Override
    public String getTarget(String topic, String unit, String lane) {
        if (unit == null || unit.isEmpty()) {
            if (lane == null || lane.isEmpty()) {
                return topic;
            }
            return topic + LANE + lane;
        } else if (lane == null || lane.isEmpty()) {
            return topic + UNIT + unit;
        }
        return topic + UNIT + unit + LANE + lane;
    }

    @Override
    public String getSource(String topic) {
        int pos = topic.lastIndexOf(UNIT);
        if (pos > 0) {
            return topic.substring(0, pos);
        }
        pos = topic.lastIndexOf(LANE);
        if (pos > 0) {
            return topic.substring(0, pos);
        }
        return topic;
    }
}
