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
package com.jd.live.agent.implement.service.policy.multilive;

import com.jd.live.agent.core.config.SyncConfig;
import com.jd.live.agent.core.extension.annotation.ConditionalOnProperty;
import com.jd.live.agent.core.extension.annotation.Extension;
import com.jd.live.agent.core.inject.annotation.Config;
import com.jd.live.agent.core.inject.annotation.Injectable;
import com.jd.live.agent.core.parser.TypeReference;
import com.jd.live.agent.core.util.http.HttpResponse;
import com.jd.live.agent.core.util.http.HttpUtils;
import com.jd.live.agent.governance.config.GovernanceConfig;
import com.jd.live.agent.governance.policy.PolicySubscriber;
import com.jd.live.agent.governance.policy.listener.ServiceEvent;
import com.jd.live.agent.governance.policy.service.MergePolicy;
import com.jd.live.agent.governance.policy.service.Service;
import com.jd.live.agent.governance.service.sync.SyncKey.ServiceKey;
import com.jd.live.agent.governance.service.sync.SyncResponse;
import com.jd.live.agent.governance.service.sync.api.ApiResponse;
import com.jd.live.agent.governance.service.sync.api.ApiResult;
import com.jd.live.agent.governance.service.sync.http.AbstractServiceHttpSyncer;
import com.jd.live.agent.implement.service.policy.multilive.config.LiveSyncConfig;

import java.io.IOException;

/**
 * LiveServiceSyncer is responsible for synchronizing live service policies from a multilive control plane.
 */
@Injectable
@Extension("LiveServiceSyncer")
@ConditionalOnProperty(name = SyncConfig.SYNC_LIVE_SPACE_TYPE, value = "multilive")
@ConditionalOnProperty(name = SyncConfig.SYNC_LIVE_SPACE_SERVICE, matchIfMissing = true)
@ConditionalOnProperty(name = GovernanceConfig.CONFIG_LIVE_ENABLED, matchIfMissing = true)
public class LiveServiceHttpSyncer extends AbstractServiceHttpSyncer<ServiceKey> {

    @Config(SyncConfig.SYNC_LIVE_SPACE)
    private LiveSyncConfig syncConfig = new LiveSyncConfig();

    public LiveServiceHttpSyncer() {
        name = "live-service-multilive-syncer";
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
        event.setMergePolicy(MergePolicy.LIVE);
    }

    @Override
    protected SyncResponse<Service> getResponse(SyncConfig config, String uri) throws IOException {
        HttpResponse<ApiResponse<ApiResult<Service>>> response = HttpUtils.get(uri,
                conn -> configure(config, conn),
                reader -> jsonParser.read(reader, new TypeReference<ApiResponse<ApiResult<Service>>>() {
                }));
        return ApiResponse.from(response).asSyncResponse(ApiResult::asSyncResponse);
    }
}
