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
package com.jd.live.agent.implement.service.policy.microservice;

import com.jd.live.agent.core.config.SyncConfig;
import com.jd.live.agent.core.extension.annotation.ConditionalOnProperty;
import com.jd.live.agent.core.extension.annotation.Extension;
import com.jd.live.agent.core.inject.annotation.Config;
import com.jd.live.agent.core.inject.annotation.Injectable;
import com.jd.live.agent.governance.config.GovernanceConfig;
import com.jd.live.agent.governance.service.sync.http.AbstractLaneSpaceHttpSyncer;
import com.jd.live.agent.implement.service.policy.microservice.config.MicroServiceSyncConfig;

/**
 * LaneSpaceSyncer is responsible for synchronizing lane spaces from a microservice control plane.
 */
@Injectable
@Extension("LaneSpaceSyncer")
@ConditionalOnProperty(name = SyncConfig.SYNC_LANE_SPACE_TYPE, value = "jmsf")
@ConditionalOnProperty(value = GovernanceConfig.CONFIG_LANE_ENABLED, matchIfMissing = true)
public class LaneSpaceHttpSyncer extends AbstractLaneSpaceHttpSyncer {

    @Config(SyncConfig.SYNC_LANE_SPACE)
    private MicroServiceSyncConfig syncConfig = new MicroServiceSyncConfig();

    public LaneSpaceHttpSyncer() {
        name = "lane-space-jmsf-syncer";
    }

    @Override
    protected SyncConfig getSyncConfig() {
        return syncConfig;
    }

}
