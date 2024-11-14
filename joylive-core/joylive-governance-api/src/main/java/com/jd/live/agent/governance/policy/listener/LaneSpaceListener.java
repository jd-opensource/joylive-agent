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

import com.jd.live.agent.core.config.ConfigEvent;
import com.jd.live.agent.core.parser.ObjectParser;
import com.jd.live.agent.governance.policy.GovernancePolicy;
import com.jd.live.agent.governance.policy.PolicySupervisor;
import com.jd.live.agent.governance.policy.lane.LaneSpace;

import java.util.ArrayList;
import java.util.List;

import static com.jd.live.agent.core.util.CollectionUtils.filter;

/**
 * A listener class for lane space configuration updates that extends the AbstractListener class.
 */
public class LaneSpaceListener extends AbstractListener<LaneSpace> {

    public LaneSpaceListener(PolicySupervisor supervisor, ObjectParser parser) {
        super(LaneSpace.class, supervisor, parser);
    }

    @Override
    protected void updateItems(GovernancePolicy policy, List<LaneSpace> items, ConfigEvent event) {
        policy.setLaneSpaces(items);
    }

    @Override
    protected void updateItem(GovernancePolicy policy, LaneSpace item, ConfigEvent event) {
        List<LaneSpace> spaces = policy.getLaneSpaces() == null ? new ArrayList<>() : policy.getLaneSpaces();
        filter(spaces, space -> !space.getId().equals(item.getId()));
        spaces.add(item);
        policy.setLaneSpaces(spaces);
    }

    @Override
    protected void deleteItem(GovernancePolicy policy, ConfigEvent event) {
        if (event.getName() == null) {
            return;
        }
        List<LaneSpace> spaces = policy.getLaneSpaces();
        if (spaces != null) {
            filter(spaces, space -> !space.getId().equals(event.getName()));
        }

    }

}
