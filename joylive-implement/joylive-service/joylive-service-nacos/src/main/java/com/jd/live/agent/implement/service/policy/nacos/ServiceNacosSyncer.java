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
import com.jd.live.agent.governance.config.GovernanceConfig;
import com.jd.live.agent.governance.config.SyncConfig;
import com.jd.live.agent.governance.policy.PolicySubscription;
import com.jd.live.agent.governance.policy.service.MergePolicy;
import com.jd.live.agent.governance.policy.service.Service;
import com.jd.live.agent.governance.probe.HealthProbe;
import com.jd.live.agent.governance.service.sync.AbstractServiceSyncer;
import com.jd.live.agent.governance.service.sync.Syncer;
import com.jd.live.agent.governance.subscription.policy.listener.ServiceEvent;
import com.jd.live.agent.implement.service.config.nacos.client.NacosClientApi;
import com.jd.live.agent.implement.service.config.nacos.client.NacosClientFactory;
import com.jd.live.agent.implement.service.policy.nacos.config.NacosSyncConfig;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * ServiceNacosSyncer is responsible for synchronizing live service policies from nacos.
 */
@Injectable
@Extension("ServiceNacosSyncer")
@ConditionalOnProperty(name = SyncConfig.SYNC_MICROSERVICE_TYPE, value = "nacos")
@ConditionalOnProperty(name = GovernanceConfig.CONFIG_FLOW_CONTROL_ENABLED, matchIfMissing = true)
public class ServiceNacosSyncer extends AbstractServiceSyncer<NacosServiceKey> {

    @Config(SyncConfig.SYNC_MICROSERVICE)
    private NacosSyncConfig syncConfig = new NacosSyncConfig();

    @Inject(HealthProbe.NACOS)
    private HealthProbe probe;

    @Inject(Timer.COMPONENT_TIMER)
    private Timer timer;

    private NacosClientApi client;

    public ServiceNacosSyncer() {
        name = "LiveAgent-service-nacos-syncer";
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
        return new Template(syncConfig.getNacos().getServiceKeyTemplate());
    }

    @Override
    protected NacosServiceKey createServiceKey(PolicySubscription subscriber) {
        Map<String, Object> context = new HashMap<>();
        context.put("name", subscriber.getName());
        context.put("space", application.getService().getNamespace());
        String dataId = template.render(context);
        return new NacosServiceKey(subscriber, dataId, syncConfig.getNacos().getServiceGroup());
    }

    @Override
    protected Syncer<NacosServiceKey, Service> createSyncer() {
        return client.createSyncer(this::parse);
    }

    @Override
    protected void configure(ServiceEvent event) {
        event.setMergePolicy(MergePolicy.FLOW_CONTROL);
    }

}
