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
package com.jd.live.agent.governance.service.sync.http;

import com.jd.live.agent.core.config.AgentPath;
import com.jd.live.agent.core.parser.ObjectParser;
import com.jd.live.agent.core.parser.TypeReference;
import com.jd.live.agent.core.util.template.Template;
import com.jd.live.agent.governance.policy.live.db.LiveDatabaseSpec;
import com.jd.live.agent.governance.service.sync.*;
import com.jd.live.agent.governance.service.sync.SyncAddress.LiveSpaceAddress;
import com.jd.live.agent.governance.service.sync.SyncKey.HttpSyncKey;
import com.jd.live.agent.governance.service.sync.SyncKey.LiveSpaceKey;
import com.jd.live.agent.governance.service.sync.api.ApiResponse;
import lombok.Getter;

import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;

/**
 * An abstract class that provides a base implementation for synchronizing live database with an HTTP service.
 */
public abstract class AbstractLiveDatabaseHttpSyncer extends AbstractLiveDatabaseSyncer<AbstractLiveDatabaseHttpSyncer.HttpLiveDatabaseKey> {

    protected static final String SPACE_ID = "space_id";

    protected static final String SPACE_VERSION = "space_version";

    protected HttpWatcher watcher;

    @Override
    protected Template createTemplate() {
        return new Template(((LiveSpaceAddress) getSyncConfig()).getLiveSpaceUrl());
    }


    @Override
    protected HttpLiveDatabaseKey createSpaceKey(String spaceId) {
        return new HttpLiveDatabaseKey(new HttpResource() {
            @Override
            public String getId() {
                return spaceId;
            }

            @Override
            public String getUrl() {
                Subscription<AbstractLiveDatabaseHttpSyncer.HttpLiveDatabaseKey, LiveDatabaseSpec> subscription = subscriptions.get(spaceId);
                Map<String, Object> context = new HashMap<>(2);
                context.put(SPACE_ID, spaceId);
                context.put(SPACE_VERSION, subscription == null ? 0 : subscription.getVersion());
                context.put(APPLICATION, application.getName());
                return template.render(context);
            }
        });
    }

    @Override
    protected Syncer<HttpLiveDatabaseKey, LiveDatabaseSpec> createSyncer() {
        watcher = creatWatcher();
        return watcher.createSyncer(this::parseDatabase);
    }

    @Override
    protected SyncResponse<LiveDatabaseSpec> parseDatabase(HttpLiveDatabaseKey key, String config) {
        if (config == null || config.isEmpty()) {
            return new SyncResponse<>(SyncStatus.NOT_FOUND, null);
        }
        ApiResponse<LiveDatabaseSpec> response = parser.read(new StringReader(config), new TypeReference<ApiResponse<LiveDatabaseSpec>>() {
        });
        saveConfig(response, parser, getFileName(key.getId()));
        return response.asSyncResponse();
    }

    protected HttpWatcher creatWatcher() {
        return new HttpWatcher(getType(), getSyncConfig(), application);
    }

    /**
     * Saves API response data to a local configuration file if the response is successful.
     *
     * @param response the API response to process (must not be {@code null})
     * @param parser   the object parser used to serialize response data (must not be {@code null})
     * @param name     the filename to use for saving the configuration (must not be {@code null} or empty)
     */
    private void saveConfig(ApiResponse<LiveDatabaseSpec> response, ObjectParser parser, String name) {
        // save config to local file
        if (response.getError() == null) {
            saveConfig(response, parser, AgentPath.DIR_POLICY_LIVE_DATABASE, name);
        }
    }

    @Getter
    protected static class HttpLiveDatabaseKey extends LiveSpaceKey implements HttpSyncKey {

        private final HttpResource resource;

        public HttpLiveDatabaseKey(HttpResource resource) {
            super(resource.getId());
            this.resource = resource;
        }

        @Override
        public String getUrl() {
            return resource.getUrl();
        }
    }
}
