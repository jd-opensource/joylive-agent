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

import com.jd.live.agent.core.config.SyncConfig;
import com.jd.live.agent.core.extension.annotation.ConditionalOnProperty;
import com.jd.live.agent.core.extension.annotation.Extension;
import com.jd.live.agent.core.inject.annotation.Config;
import com.jd.live.agent.core.inject.annotation.Injectable;
import com.jd.live.agent.core.util.Close;
import com.jd.live.agent.core.util.template.Template;
import com.jd.live.agent.governance.config.GovernanceConfig;
import com.jd.live.agent.governance.policy.PolicySubscriber;
import com.jd.live.agent.governance.policy.listener.ServiceEvent;
import com.jd.live.agent.governance.policy.service.MergePolicy;
import com.jd.live.agent.governance.policy.service.Service;
import com.jd.live.agent.governance.service.sync.AbstractServiceSyncer;
import com.jd.live.agent.governance.service.sync.SyncKey.ServiceKey;
import com.jd.live.agent.governance.service.sync.Syncer;
import com.jd.live.agent.implement.service.policy.nacos.client.NacosClient;
import com.jd.live.agent.implement.service.policy.nacos.client.NacosSyncKey;
import com.jd.live.agent.implement.service.policy.nacos.config.NacosSyncConfig;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

import static com.jd.live.agent.implement.service.policy.nacos.ServiceNacosSyncer.NacosServiceKey;

/**
 * ServiceNacosSyncer is responsible for synchronizing live service policies from nacos.
 */
@Injectable
@Extension("ServiceNacosSyncer")
@ConditionalOnProperty(name = SyncConfig.SYNC_LIVE_SPACE_TYPE, value = "nacos")
@ConditionalOnProperty(name = SyncConfig.SYNC_LIVE_SPACE_SERVICE, matchIfMissing = true)
@ConditionalOnProperty(name = GovernanceConfig.CONFIG_LIVE_ENABLED, matchIfMissing = true)
public class ServiceNacosSyncer extends AbstractServiceSyncer<NacosServiceKey> {

    @Config(SyncConfig.SYNC_LIVE_SPACE)
    private NacosSyncConfig syncConfig = new NacosSyncConfig();

    private NacosClient client;

    public ServiceNacosSyncer() {
        name = "service-nacos-syncer";
    }

    @Override
    protected void startSync() throws Exception {
        client = new NacosClient(syncConfig);
        client.connect();
        super.startSync();
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
        return new Template(syncConfig.getServiceKeyTemplate());
    }

    @Override
    protected NacosServiceKey createServiceKey(PolicySubscriber subscriber) {
        Map<String, Object> context = new HashMap<>();
        context.put("name", subscriber.getName());
        context.put("space", application.getService().getNamespace());
        String dataId = template.evaluate(context);
        return new NacosServiceKey(subscriber, dataId, syncConfig.getServiceGroup());
    }

    @Override
    protected Syncer<NacosServiceKey, Service> createSyncer() {
        return client.createSyncer(this::parse);
    }

    @Override
    protected void configure(ServiceEvent event) {
        event.setMergePolicy(MergePolicy.FLOW_CONTROL);
    }

    @Getter
    protected static class NacosServiceKey extends ServiceKey implements NacosSyncKey {

        private final String dataId;

        private final String group;

        public NacosServiceKey(PolicySubscriber subscriber, String dataId, String group) {
            super(subscriber);
            this.dataId = dataId;
            this.group = group;
        }
    }

}
