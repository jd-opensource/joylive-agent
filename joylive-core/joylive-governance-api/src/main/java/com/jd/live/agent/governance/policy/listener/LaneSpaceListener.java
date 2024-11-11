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
package com.jd.live.agent.governance.policy.listener;

import com.jd.live.agent.core.config.Configuration;
import com.jd.live.agent.core.parser.ObjectParser;
import com.jd.live.agent.core.parser.TypeReference;
import com.jd.live.agent.governance.policy.GovernancePolicy;
import com.jd.live.agent.governance.policy.PolicySupervisor;
import com.jd.live.agent.governance.policy.lane.LaneSpace;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

public class LaneSpaceListener extends AbstractListener<List<LaneSpace>> {

    private final ObjectParser parser;

    public LaneSpaceListener(PolicySupervisor supervisor, ObjectParser parser) {
        super(supervisor);
        this.parser = parser;
    }

    @SuppressWarnings("unchecked")
    @Override
    protected List<LaneSpace> parse(Configuration config) {
        Object value = config.getValue();
        if (value instanceof List) {
            return (List<LaneSpace>) value;
        } else if (value instanceof LaneSpace) {
            List<LaneSpace> result = new ArrayList<>();
            result.add((LaneSpace) value);
            return result;
        } else if (value instanceof String) {
            String str = (String) value;
            str = str.trim();
            if (str.isEmpty()) {
                return new ArrayList<>();
            } else if (str.startsWith("[")) {
                return parser.read(new StringReader(str), new TypeReference<List<LaneSpace>>() {
                });
            } else {
                List<LaneSpace> result = new ArrayList<>();
                result.add(parser.read(new StringReader(str), LaneSpace.class));
                return result;
            }
        } else {
            return new ArrayList<>();
        }
    }

    @Override
    protected void update(GovernancePolicy policy, List<LaneSpace> spaces, String watcher) {
        policy.setLaneSpaces(spaces);
    }
}
