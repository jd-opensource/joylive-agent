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

import com.jd.live.agent.core.extension.annotation.ConditionalOnProperty;
import com.jd.live.agent.core.extension.annotation.Extension;
import com.jd.live.agent.core.inject.annotation.Config;
import com.jd.live.agent.core.inject.annotation.Inject;
import com.jd.live.agent.core.inject.annotation.Injectable;
import com.jd.live.agent.core.util.Close;
import com.jd.live.agent.core.util.Futures;
import com.jd.live.agent.core.util.template.Template;
import com.jd.live.agent.core.util.time.Timer;
import com.jd.live.agent.governance.annotation.ConditionalOnLiveEnabled;
import com.jd.live.agent.governance.config.SyncConfig;
import com.jd.live.agent.governance.policy.live.LiveSpace;
import com.jd.live.agent.governance.probe.HealthProbe;
import com.jd.live.agent.governance.service.sync.AbstractLiveSpaceSyncer;
import com.jd.live.agent.governance.service.sync.Syncer;
import com.jd.live.agent.governance.service.sync.api.ApiSpace;
import com.jd.live.agent.implement.service.config.nacos.client.NacosClientApi;
import com.jd.live.agent.implement.service.config.nacos.client.NacosClientFactory;
import com.jd.live.agent.implement.service.policy.nacos.config.NacosSyncConfig;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * LiveSpaceNacosSyncer is responsible for synchronizing live spaces from nacos.
 */
@Injectable
@Extension("LiveSpaceNacosSyncer")
@ConditionalOnLiveEnabled
@ConditionalOnProperty(name = SyncConfig.SYNC_LIVE_SPACE_TYPE, value = "nacos")
public class LiveSpaceNacosSyncer extends AbstractLiveSpaceSyncer<NacosLiveSpaceKey, NacosLiveSpaceKey> {

    @Config(SyncConfig.SYNC_LIVE_SPACE)
    private NacosSyncConfig syncConfig = new NacosSyncConfig();

    @Inject(HealthProbe.NACOS)
    private HealthProbe probe;

    @Inject(Timer.COMPONENT_TIMER)
    private Timer timer;

    private NacosClientApi client;

    public LiveSpaceNacosSyncer() {
        name = "LiveAgent-live-space-nacos-syncer";
    }

    @Override
    protected CompletableFuture<Void> doStart() {
        try {
            client = NacosClientFactory.create(syncConfig.getProperties(), probe, timer);
            client.connect();
        } catch (Exception e) {
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
        return new Template(syncConfig.getNacos().getLiveSpaceKeyTemplate());
    }

    @Override
    protected NacosLiveSpaceKey createSpaceListKey() {
        return new NacosLiveSpaceKey(null, syncConfig.getNacos().getLiveSpacesKey(), syncConfig.getNacos().getLiveSpaceGroup());
    }

    @Override
    protected NacosLiveSpaceKey createSpaceKey(String spaceId) {
        Map<String, Object> context = new HashMap<>();
        context.put("id", spaceId);
        String dataId = template.render(context);
        return new NacosLiveSpaceKey(spaceId, dataId, syncConfig.getNacos().getLiveSpaceGroup());
    }

    @Override
    protected Syncer<NacosLiveSpaceKey, List<ApiSpace>> createSpaceListSyncer() {
        return client.createSyncer(this::parseSpaceList);
    }

    @Override
    protected Syncer<NacosLiveSpaceKey, LiveSpace> createSyncer() {
        return client.createSyncer(this::parseSpace);
    }

}
