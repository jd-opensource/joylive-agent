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

import com.jd.live.agent.bootstrap.util.Inclusion;
import com.jd.live.agent.core.extension.annotation.Extension;
import com.jd.live.agent.core.inject.annotation.Inject;
import com.jd.live.agent.core.inject.annotation.Injectable;
import com.jd.live.agent.governance.config.GovernanceConfig;
import com.jd.live.agent.governance.config.TransmitConfig;
import com.jd.live.agent.governance.context.bag.CargoRequire;

import java.util.Set;

/**
 * LiveCargoRequire is an implementation of the CargoRequire interface that provides
 * the necessary cargo requirements for live streaming scenarios. It uses
 * a LiveConfig instance to determine the specific keys required for space ID,
 * rule ID, and variables, as well as a common prefix for live-related configurations.
 *
 * @since 1.0.0
 */
@Injectable
@Extension("LiveCargoRequire")
public class LiveCargoRequire implements CargoRequire {

    @Inject(GovernanceConfig.COMPONENT_GOVERNANCE_CONFIG)
    private GovernanceConfig governanceConfig;

    public LiveCargoRequire() {
    }

    public LiveCargoRequire(GovernanceConfig governanceConfig) {
        this.governanceConfig = governanceConfig;
    }

    @Override
    public Set<String> getNames() {
        return governanceConfig.getTransmitConfig().getKeys();
    }

    @Override
    public Set<String> getPrefixes() {
        return governanceConfig.getTransmitConfig().getPrefixes();
    }

    @Override
    public boolean test(String name) {
        TransmitConfig config = governanceConfig.getTransmitConfig();
        return Inclusion.test(config.getKeys(), config.getPrefixes(), false, name);
    }
}
