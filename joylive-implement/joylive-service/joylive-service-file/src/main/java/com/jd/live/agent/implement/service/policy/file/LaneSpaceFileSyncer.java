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
package com.jd.live.agent.implement.service.policy.file;

import com.jd.live.agent.core.config.ConfigWatcher;
import com.jd.live.agent.core.config.SyncConfig;
import com.jd.live.agent.core.extension.annotation.ConditionalOnProperty;
import com.jd.live.agent.core.extension.annotation.Extension;
import com.jd.live.agent.core.inject.annotation.Config;
import com.jd.live.agent.core.inject.annotation.Injectable;
import com.jd.live.agent.core.service.sync.AbstractFileSyncer;
import com.jd.live.agent.governance.config.GovernanceConfig;
import lombok.Getter;

/**
 * LiveSpaceFileSyncer
 *
 * @since 1.0.0
 */
@Getter
@Injectable
@Extension("LaneSpaceFileSyncer")
@ConditionalOnProperty(name = SyncConfig.SYNC_LANE_SPACE_TYPE, value = "file")
@ConditionalOnProperty(value = GovernanceConfig.CONFIG_LIVE_ENABLED, matchIfMissing = true)
public class LaneSpaceFileSyncer extends AbstractFileSyncer {

    private static final String CONFIG_LANE_SPACE = "lanes.json";

    @Config(SyncConfig.SYNC_LANE_SPACE)
    private SyncConfig syncConfig = new SyncConfig();

    @Override
    public String getType() {
        return ConfigWatcher.TYPE_LANE_SPACE;
    }

    @Override
    protected String getDefaultResource() {
        return CONFIG_LANE_SPACE;
    }
}
