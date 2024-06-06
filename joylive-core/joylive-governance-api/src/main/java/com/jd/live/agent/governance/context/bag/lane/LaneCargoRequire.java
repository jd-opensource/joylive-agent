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
package com.jd.live.agent.governance.context.bag.lane;

import com.jd.live.agent.core.Constants;
import com.jd.live.agent.core.extension.annotation.Extension;
import com.jd.live.agent.core.inject.annotation.Inject;
import com.jd.live.agent.governance.config.LaneConfig;
import com.jd.live.agent.governance.context.bag.CargoRequire;

/**
 * LaneCargoRequire is an implementation of the CargoRequire interface that provides
 * the necessary cargo requirements for a lane-specific configuration. It uses
 * a LaneConfig instance to determine the specific keys required for space ID and lane code.
 */
@Extension("LaneCargoRequire")
public class LaneCargoRequire implements CargoRequire {

    @Inject(LaneConfig.COMPONENT_LANE_CONFIG)
    private LaneConfig laneConfig;

    @Override
    public String[] getNames() {
        String spaceIdKey = laneConfig == null ? Constants.LABEL_LANE_SPACE_ID : laneConfig.getSpaceIdKey();
        String codeKey = laneConfig == null ? Constants.LABEL_LANE : laneConfig.getLaneKey();
        return new String[]{spaceIdKey, codeKey};
    }
}
