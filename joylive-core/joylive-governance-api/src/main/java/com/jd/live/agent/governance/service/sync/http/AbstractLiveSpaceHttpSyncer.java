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

import com.jd.live.agent.core.inject.annotation.Inject;
import com.jd.live.agent.core.instance.Application;
import com.jd.live.agent.core.parser.ObjectParser;
import com.jd.live.agent.core.parser.TypeReference;
import com.jd.live.agent.core.util.template.Template;
import com.jd.live.agent.governance.policy.live.LiveSpace;
import com.jd.live.agent.governance.service.sync.*;
import com.jd.live.agent.governance.service.sync.SyncAddress.LiveSpaceAddress;
import com.jd.live.agent.governance.service.sync.SyncKey.HttpSyncKey;
import com.jd.live.agent.governance.service.sync.SyncKey.LiveSpaceKey;
import com.jd.live.agent.governance.service.sync.api.ApiResponse;
import com.jd.live.agent.governance.service.sync.api.ApiSpace;
import lombok.Getter;

import java.io.StringReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.jd.live.agent.governance.service.sync.http.AbstractLiveSpaceHttpSyncer.HttpLiveSpaceKey;

/**
 * An abstract class that provides a base implementation for synchronizing live spaces with an HTTP service.
 */
public abstract class AbstractLiveSpaceHttpSyncer extends AbstractLiveSpaceSyncer<HttpLiveSpaceKey, HttpLiveSpaceKey> {

    protected static final String SPACE_ID = "space_id";

    protected static final String SPACE_VERSION = "space_version";

    @Inject(Application.COMPONENT_APPLICATION)
    protected Application application;

    @Inject(ObjectParser.JSON)
    protected ObjectParser parser;

    protected HttpWatcher watcher;

    @Override
    protected Template createTemplate() {
        return new Template(((LiveSpaceAddress) getSyncConfig()).getLiveSpaceUrl());
    }

    @Override
    protected HttpLiveSpaceKey createSpaceListKey() {
        return new HttpLiveSpaceKey(new HttpResource() {
            @Override
            public String getId() {
                return "";
            }

            @Override
            public String getUrl() {
                return ((LiveSpaceAddress) getSyncConfig()).getLiveSpacesUrl();
            }
        });
    }

    @Override
    protected HttpLiveSpaceKey createSpaceKey(String spaceId) {
        return new HttpLiveSpaceKey(new HttpResource() {
            @Override
            public String getId() {
                return spaceId;
            }

            @Override
            public String getUrl() {
                Subscription<HttpLiveSpaceKey, LiveSpace> subscription = subscriptions.get(spaceId);
                Map<String, Object> context = new HashMap<>(2);
                context.put(SPACE_ID, spaceId);
                context.put(SPACE_VERSION, subscription == null ? 0 : subscription.getVersion());
                return template.evaluate(context);
            }
        });
    }

    @Override
    protected Syncer<HttpLiveSpaceKey, List<ApiSpace>> createSpaceListSyncer() {
        // This is called after createSyncer
        return watcher.createSyncer(this::parseSpaceList);
    }

    @Override
    protected Syncer<HttpLiveSpaceKey, LiveSpace> createSyncer() {
        watcher = creatWatcher();
        return watcher.createSyncer(this::parseSpace);
    }

    @Override
    protected SyncResponse<List<ApiSpace>> parseSpaceList(String config) {
        if (config == null || config.isEmpty()) {
            return new SyncResponse<>(SyncStatus.NOT_FOUND, null);
        }
        ApiResponse<List<ApiSpace>> response = parser.read(new StringReader(config), new TypeReference<ApiResponse<List<ApiSpace>>>() {
        });
        return response.asSyncResponse();
    }

    @Override
    protected SyncResponse<LiveSpace> parseSpace(String config) {
        if (config == null || config.isEmpty()) {
            return new SyncResponse<>(SyncStatus.NOT_FOUND, null);
        }
        ApiResponse<LiveSpace> response = parser.read(new StringReader(config), new TypeReference<ApiResponse<LiveSpace>>() {
        });
        return response.asSyncResponse();
    }

    protected HttpWatcher creatWatcher() {
        return new HttpWatcher(getType(), getSyncConfig(), application);
    }

    @Getter
    protected static class HttpLiveSpaceKey extends LiveSpaceKey implements HttpSyncKey {

        private final HttpResource resource;

        public HttpLiveSpaceKey(HttpResource resource) {
            super(resource.getId());
            this.resource = resource;
        }

        @Override
        public String getUrl() {
            return resource.getUrl();
        }
    }
}
