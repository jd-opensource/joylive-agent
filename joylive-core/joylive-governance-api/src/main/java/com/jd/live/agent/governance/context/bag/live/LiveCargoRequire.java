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
package com.jd.live.agent.governance.context.bag.live;

import com.jd.live.agent.core.Constants;
import com.jd.live.agent.core.extension.annotation.Extension;
import com.jd.live.agent.core.inject.annotation.Inject;
import com.jd.live.agent.governance.config.LiveConfig;
import com.jd.live.agent.governance.context.bag.CargoRequire;

/**
 * LiveCargoRequire is an implementation of the CargoRequire interface that provides
 * the necessary cargo requirements for live streaming scenarios. It uses
 * a LiveConfig instance to determine the specific keys required for space ID,
 * rule ID, and variables, as well as a common prefix for live-related configurations.
 */
@Extension("LiveCargoRequire")
public class LiveCargoRequire implements CargoRequire {

    @Inject(LiveConfig.COMPONENT_LIVE_CONFIG)
    private LiveConfig liveConfig;

    @Override
    public String[] getNames() {
        String spaceIdKey = liveConfig == null ? Constants.LABEL_LIVE_SPACE_ID : liveConfig.getSpaceIdKey();
        String ruleIdKey = liveConfig == null ? Constants.LABEL_RULE_ID : liveConfig.getRuleIdKey();
        String variableKey = liveConfig == null ? Constants.LABEL_VARIABLE : liveConfig.getVariableKey();
        return new String[]{spaceIdKey, ruleIdKey, variableKey};
    }

    @Override
    public String[] getPrefixes() {
        return new String[]{Constants.LABEL_LIVE_PREFIX};
    }
}
