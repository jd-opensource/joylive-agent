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

import com.jd.live.agent.core.config.ConfigWatcher;
import com.jd.live.agent.core.config.SyncConfig;
import com.jd.live.agent.core.extension.annotation.ConditionalOnProperty;
import com.jd.live.agent.core.extension.annotation.Extension;
import com.jd.live.agent.core.inject.annotation.Config;
import com.jd.live.agent.core.inject.annotation.Injectable;
import com.jd.live.agent.core.parser.TypeReference;
import com.jd.live.agent.governance.config.GovernanceConfig;
import com.jd.live.agent.governance.policy.live.LiveSpace;
import com.jd.live.agent.governance.service.sync.SyncKey.FileKey;
import com.jd.live.agent.governance.service.sync.Syncer;
import com.jd.live.agent.governance.service.sync.file.AbstractFileSyncer;
import com.jd.live.agent.governance.service.sync.file.FileWatcher;
import lombok.Getter;

import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.util.List;

/**
 * LiveSpaceFileSyncer
 *
 * @since 1.0.0
 */
@Getter
@Injectable
@Extension("LiveSpaceFileSyncer")
@ConditionalOnProperty(name = SyncConfig.SYNC_LIVE_SPACE_TYPE, value = "file")
@ConditionalOnProperty(name = GovernanceConfig.CONFIG_LIVE_ENABLED, matchIfMissing = true)
public class LiveSpaceFileSyncer extends AbstractFileSyncer<List<LiveSpace>> {

    private static final String CONFIG_LIVE_SPACE = "livespaces.json";

    @Config(SyncConfig.SYNC_LIVE_SPACE)
    private SyncConfig syncConfig = new SyncConfig();

    public LiveSpaceFileSyncer() {
        name = "live-space-file-syncer";
    }

    @Override
    public String getType() {
        return ConfigWatcher.TYPE_LIVE_SPACE;
    }

    @Override
    protected String getDefaultResource() {
        return CONFIG_LIVE_SPACE;
    }

    @Override
    protected Syncer<FileKey, List<LiveSpace>> createSyncer() {
        fileWatcher = new FileWatcher(getName(), getSyncConfig(), publisher);
        return fileWatcher.createSyncer(file,
                data -> parser.read(new InputStreamReader(new ByteArrayInputStream(data)), new TypeReference<List<LiveSpace>>() {
                }));
    }
}
