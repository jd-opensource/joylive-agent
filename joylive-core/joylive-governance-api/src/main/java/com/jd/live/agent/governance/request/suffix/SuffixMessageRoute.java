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
package com.jd.live.agent.governance.request.suffix;

import com.jd.live.agent.core.extension.annotation.Extension;
import com.jd.live.agent.governance.request.MessageRoute;

@Extension("suffix")
public class SuffixMessageRoute implements MessageRoute {

    private static final String LANE = "-lane-";

    private static final String UNIT = "-unit-";

    @Override
    public String getTarget(String topic, String unit, String lane) {
        int unitLength = unit != null ? unit.length() : 0;
        int lanLength = lane != null ? lane.length() : 0;
        int length = topic.length() + unitLength + lanLength;
        if (length == topic.length()) {
            return topic;
        }

        // Initialize the StringBuilder with the calculated final length
        StringBuilder target = new StringBuilder(length).append(topic);
        if (unitLength > 0) {
            target.append(UNIT).append(unit);
        }
        if (lanLength > 0) {
            target.append(LANE).append(lane);
        }

        return target.toString();
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
