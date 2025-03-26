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

import com.jd.live.agent.core.config.AgentPath;
import com.jd.live.agent.core.extension.annotation.ConditionalOnProperty;
import com.jd.live.agent.core.extension.annotation.Extension;
import com.jd.live.agent.core.inject.annotation.Config;
import com.jd.live.agent.core.inject.annotation.Injectable;
import com.jd.live.agent.core.parser.ObjectParser;
import com.jd.live.agent.core.parser.TypeReference;
import com.jd.live.agent.core.util.http.HttpStatus;
import com.jd.live.agent.governance.config.GovernanceConfig;
import com.jd.live.agent.governance.config.SyncConfig;
import com.jd.live.agent.governance.policy.live.LiveSpace;
import com.jd.live.agent.governance.service.sync.SyncResponse;
import com.jd.live.agent.governance.service.sync.SyncStatus;
import com.jd.live.agent.governance.service.sync.api.ApiResponse;
import com.jd.live.agent.governance.service.sync.api.ApiResult;
import com.jd.live.agent.governance.service.sync.api.ApiSpace;
import com.jd.live.agent.governance.service.sync.http.AbstractLiveSpaceHttpSyncer;
import com.jd.live.agent.implement.service.policy.multilive.config.LiveSyncConfig;

import java.io.StringReader;
import java.util.List;

/**
 * LiveSpaceSyncer is responsible for synchronizing live spaces from a multilive control plane.
 */
@Injectable
@Extension("LiveSpaceSyncer")
@ConditionalOnProperty(name = SyncConfig.SYNC_LIVE_SPACE_TYPE, value = "multilive")
@ConditionalOnProperty(value = GovernanceConfig.CONFIG_LIVE_ENABLED, matchIfMissing = true)
public class LiveSpaceHttpSyncer extends AbstractLiveSpaceHttpSyncer {

    @Config(SyncConfig.SYNC_LIVE_SPACE)
    private LiveSyncConfig syncConfig = new LiveSyncConfig();

    public LiveSpaceHttpSyncer() {
        name = "LiveAgent-space-multilive-syncer";
    }

    @Override
    protected SyncConfig getSyncConfig() {
        return syncConfig;
    }

    @Override
    protected SyncResponse<List<ApiSpace>> parseSpaceList(HttpLiveSpaceKey key, String config) {
        if (config == null || config.isEmpty()) {
            return new SyncResponse<>(SyncStatus.NOT_FOUND, null);
        }
        ApiResponse<ApiResult<List<ApiSpace>>> response = parser.read(new StringReader(config), new TypeReference<ApiResponse<ApiResult<List<ApiSpace>>>>() {
        });
        return response.asSyncResponse(ApiResult::asSyncResponse);
    }

    @Override
    protected SyncResponse<LiveSpace> parseSpace(HttpLiveSpaceKey key, String config) {
        if (config == null || config.isEmpty()) {
            return new SyncResponse<>(SyncStatus.NOT_FOUND, null);
        }
        ApiResponse<ApiResult<LiveSpace>> response = parser.read(new StringReader(config), new TypeReference<ApiResponse<ApiResult<LiveSpace>>>() {
        });
        saveConfig(response, parser, getFileName(key.getId()));
        return response.asSyncResponse(ApiResult::asSyncResponse);
    }

    /**
     * Saves API response data to a local configuration file if the response is successful.
     *
     * @param response the API response to process (must not be {@code null})
     * @param parser   the object parser used to serialize response data (must not be {@code null})
     * @param name     the filename to use for saving the configuration (must not be {@code null} or empty)
     */
    private <T> void saveConfig(ApiResponse<ApiResult<T>> response, ObjectParser parser, String name) {
        // save config to local file
        if (response.getError() == null) {
            ApiResult<?> result = response.getResult();
            if (response.getStatus() == HttpStatus.OK) {
                saveConfig(result.getData(), parser, AgentPath.DIR_POLICY_LIVE, name);
            }
        }
    }

}
