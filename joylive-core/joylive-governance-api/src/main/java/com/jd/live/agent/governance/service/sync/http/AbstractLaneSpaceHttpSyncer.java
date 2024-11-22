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
import com.jd.live.agent.governance.policy.lane.LaneSpace;
import com.jd.live.agent.governance.service.sync.*;
import com.jd.live.agent.governance.service.sync.SyncAddress.LaneSpaceAddress;
import com.jd.live.agent.governance.service.sync.SyncKey.HttpSyncKey;
import com.jd.live.agent.governance.service.sync.SyncKey.LaneSpaceKey;
import com.jd.live.agent.governance.service.sync.api.ApiResponse;
import com.jd.live.agent.governance.service.sync.api.ApiSpace;
import lombok.Getter;

import java.io.StringReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.jd.live.agent.governance.service.sync.http.AbstractLaneSpaceHttpSyncer.HttpLaneSpaceKey;

/**
 * An abstract class that provides a base implementation for synchronizing lane spaces with an HTTP service.
 */
public abstract class AbstractLaneSpaceHttpSyncer extends AbstractLaneSpaceSyncer<HttpLaneSpaceKey> {

    protected static final String SPACE_ID = "space_id";

    protected static final String SPACE_VERSION = "space_version";

    @Inject(Application.COMPONENT_APPLICATION)
    protected Application application;

    @Inject(ObjectParser.JSON)
    protected ObjectParser parser;

    protected HttpWatcher watcher;

    @Override
    protected Template createTemplate() {
        return new Template(((LaneSpaceAddress) getSyncConfig()).getLaneSpaceUrl());
    }

    @Override
    protected HttpLaneSpaceKey createSpaceListKey() {
        return new HttpLaneSpaceKey(new HttpResource() {
            @Override
            public String getId() {
                return "";
            }

            @Override
            public String getUrl() {
                return ((LaneSpaceAddress) getSyncConfig()).getLaneSpacesUrl();
            }
        });
    }

    @Override
    protected HttpLaneSpaceKey createSpaceKey(String spaceId) {
        return new HttpLaneSpaceKey(new HttpResource() {
            @Override
            public String getId() {
                return spaceId;
            }

            @Override
            public String getUrl() {
                Subscription<HttpLaneSpaceKey, LaneSpace> subscription = subscriptions.get(spaceId);
                Map<String, Object> context = new HashMap<>(2);
                context.put(SPACE_ID, spaceId);
                context.put(SPACE_VERSION, subscription == null ? 0 : subscription.getVersion());
                return template.evaluate(context);
            }
        });
    }

    @Override
    protected Syncer<HttpLaneSpaceKey, List<ApiSpace>> createSpaceListSyncer() {
        // This is called after createSyncer
        return watcher.createSyncer(this::parseSpaceList);
    }

    @Override
    protected Syncer<HttpLaneSpaceKey, LaneSpace> createSyncer() {
        watcher = new HttpWatcher(getType(), getSyncConfig(), application);
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
    protected SyncResponse<LaneSpace> parseSpace(String config) {
        if (config == null || config.isEmpty()) {
            return new SyncResponse<>(SyncStatus.NOT_FOUND, null);
        }
        ApiResponse<LaneSpace> response = parser.read(new StringReader(config), new TypeReference<ApiResponse<LaneSpace>>() {
        });
        return response.asSyncResponse();
    }

    @Getter
    protected static class HttpLaneSpaceKey extends LaneSpaceKey implements HttpSyncKey {

        private final HttpResource resource;

        public HttpLaneSpaceKey(HttpResource resource) {
            super(resource.getId());
            this.resource = resource;
        }

        @Override
        public String getUrl() {
            return resource.getUrl();
        }
    }
}
