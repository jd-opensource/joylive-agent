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
import com.jd.live.agent.governance.policy.PolicySubscriber;
import com.jd.live.agent.governance.policy.listener.ServiceEvent;
import com.jd.live.agent.governance.policy.service.MergePolicy;
import com.jd.live.agent.governance.service.sync.SyncKey.ServiceKey;
import com.jd.live.agent.governance.service.sync.http.AbstractServiceHttpSyncer;
import com.jd.live.agent.implement.service.policy.microservice.config.MicroServiceSyncConfig;

/**
 * MicroServiceSyncer is responsible for synchronizing microservice policies from a microservice control plane.
 */
@Injectable
@Extension("MicroServiceSyncer")
@ConditionalOnProperty(name = SyncConfig.SYNC_MICROSERVICE_TYPE, value = "jmsf")
@ConditionalOnProperty(name = GovernanceConfig.CONFIG_FLOW_CONTROL_ENABLED, matchIfMissing = true)
public class ServiceHttpSyncer extends AbstractServiceHttpSyncer<ServiceKey> {

    @Config(SyncConfig.SYNC_MICROSERVICE)
    private MicroServiceSyncConfig syncConfig = new MicroServiceSyncConfig();

    public ServiceHttpSyncer() {
        name = "service-jmsf-syncer";
    }

    @Override
    protected SyncConfig getSyncConfig() {
        return syncConfig;
    }

    @Override
    protected ServiceKey createServiceKey(PolicySubscriber subscriber) {
        return new ServiceKey(subscriber);
    }

    @Override
    protected void configure(ServiceEvent event) {
        event.setMergePolicy(MergePolicy.FLOW_CONTROL);
    }
}
