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
package com.jd.live.agent.implement.service.policy.file;

import com.jd.live.agent.core.config.ConfigEvent;
import com.jd.live.agent.core.config.ConfigEvent.EventType;
import com.jd.live.agent.core.config.ConfigWatcher;
import com.jd.live.agent.core.config.SyncConfig;
import com.jd.live.agent.core.extension.annotation.ConditionalOnProperty;
import com.jd.live.agent.core.extension.annotation.Extension;
import com.jd.live.agent.core.inject.annotation.Config;
import com.jd.live.agent.core.inject.annotation.Injectable;
import com.jd.live.agent.core.parser.TypeReference;
import com.jd.live.agent.governance.policy.listener.ServiceEvent;
import com.jd.live.agent.governance.policy.service.MergePolicy;
import com.jd.live.agent.governance.policy.service.Service;
import com.jd.live.agent.governance.service.sync.SyncKey.FileKey;
import com.jd.live.agent.governance.service.sync.Syncer;
import com.jd.live.agent.governance.service.sync.file.AbstractFileSyncer;
import com.jd.live.agent.governance.service.sync.file.FileWatcher;
import lombok.Getter;

import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.util.List;

/**
 * ServiceFileSyncer
 */
@Getter
@Injectable
@Extension("ServiceFileSyncer")
@ConditionalOnProperty(name = SyncConfig.SYNC_MICROSERVICE_TYPE, value = "file")
public class ServiceFileSyncer extends AbstractFileSyncer<List<Service>> {

    private static final String CONFIG_MICROSERVICE = "microservice.json";

    @Config(SyncConfig.SYNC_MICROSERVICE)
    private SyncConfig syncConfig = new SyncConfig();

    public ServiceFileSyncer() {
        name = "service-file-syncer";
    }

    @Override
    public String getType() {
        return ConfigWatcher.TYPE_SERVICE_SPACE;
    }

    @Override
    protected String getDefaultResource() {
        return CONFIG_MICROSERVICE;
    }

    @Override
    protected ConfigEvent createEvent(List<Service> data) {
        return ServiceEvent.creator()
                .type(EventType.UPDATE_ALL)
                .value(data)
                .description(getType())
                .watcher(getName())
                .mergePolicy(MergePolicy.ALL)
                .build();
    }

    @Override
    protected Syncer<FileKey, List<Service>> createSyncer() {
        fileWatcher = new FileWatcher(getName(), getSyncConfig(), publisher);
        return fileWatcher.createSyncer(file,
                data -> parser.read(new InputStreamReader(new ByteArrayInputStream(data)), new TypeReference<List<Service>>() {
                }));
    }
}
