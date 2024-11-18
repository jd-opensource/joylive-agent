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
package com.jd.live.agent.implement.service.policy.nacos;

import com.alibaba.nacos.api.exception.NacosException;
import com.jd.live.agent.core.config.SyncConfig;
import com.jd.live.agent.core.extension.annotation.ConditionalOnProperty;
import com.jd.live.agent.core.extension.annotation.Extension;
import com.jd.live.agent.core.inject.annotation.Config;
import com.jd.live.agent.core.inject.annotation.Injectable;
import com.jd.live.agent.core.util.Close;
import com.jd.live.agent.core.util.Futures;
import com.jd.live.agent.core.util.template.Template;
import com.jd.live.agent.governance.config.GovernanceConfig;
import com.jd.live.agent.governance.policy.lane.LaneSpace;
import com.jd.live.agent.governance.service.sync.AbstractLaneSpaceSyncer;
import com.jd.live.agent.governance.service.sync.SyncKey.LaneSpaceKey;
import com.jd.live.agent.governance.service.sync.Syncer;
import com.jd.live.agent.governance.service.sync.api.ApiSpace;
import com.jd.live.agent.implement.service.policy.nacos.client.NacosClientApi;
import com.jd.live.agent.implement.service.policy.nacos.client.NacosClientFactory;
import com.jd.live.agent.implement.service.policy.nacos.config.NacosSyncConfig;
import lombok.Getter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static com.jd.live.agent.implement.service.policy.nacos.LaneSpaceNacosSyncer.NacosLaneSpaceKey;

/**
 * LaneSpaceNacosSyncer is responsible for synchronizing lane spaces policies from nacos.
 */
@Injectable
@Extension("LaneSpaceNacosSyncer")
@ConditionalOnProperty(name = SyncConfig.SYNC_LANE_SPACE_TYPE, value = "nacos")
@ConditionalOnProperty(name = GovernanceConfig.CONFIG_LANE_ENABLED, matchIfMissing = true)
public class LaneSpaceNacosSyncer extends AbstractLaneSpaceSyncer<NacosLaneSpaceKey> {

    @Config(SyncConfig.SYNC_LANE_SPACE)
    private NacosSyncConfig syncConfig = new NacosSyncConfig();

    private NacosClientApi client;

    public LaneSpaceNacosSyncer() {
        name = "lane-space-nacos-syncer";
    }

    @Override
    protected CompletableFuture<Void> doStart() {
        try {
            client = NacosClientFactory.create(syncConfig);
            client.connect();
        } catch (NacosException e) {
            return Futures.future(e);
        }
        return super.doStart();
    }

    @Override
    protected void stopSync() {
        Close.instance().close(client);
        super.stopSync();
    }

    @Override
    protected NacosSyncConfig getSyncConfig() {
        return syncConfig;
    }

    @Override
    protected Template createTemplate() {
        return new Template(syncConfig.getNacos().getLaneSpaceKeyTemplate());
    }

    @Override
    protected NacosLaneSpaceKey createSpaceListKey() {
        return new NacosLaneSpaceKey(null, syncConfig.getNacos().getLaneSpacesKey(), syncConfig.getNacos().getLaneSpaceGroup());
    }

    @Override
    protected NacosLaneSpaceKey createSpaceKey(String spaceId) {
        Map<String, Object> context = new HashMap<>();
        context.put("id", spaceId);
        String dataId = template.evaluate(context);
        return new NacosLaneSpaceKey(spaceId, dataId, syncConfig.getNacos().getLaneSpaceGroup());
    }

    @Override
    protected Syncer<NacosLaneSpaceKey, List<ApiSpace>> createSpaceListSyncer() {
        return client.createSyncer(this::parseSpaceList);
    }

    @Override
    protected Syncer<NacosLaneSpaceKey, LaneSpace> createSyncer() {
        return client.createSyncer(this::parseSpace);
    }

    @Getter
    protected static class NacosLaneSpaceKey extends LaneSpaceKey implements NacosSyncKey {

        private final String dataId;

        private final String group;

        public NacosLaneSpaceKey(String id, String dataId, String group) {
            super(id);
            this.dataId = dataId;
            this.group = group;
        }
    }

}
