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

import com.jd.live.agent.bootstrap.logger.Logger;
import com.jd.live.agent.bootstrap.logger.LoggerFactory;
import com.jd.live.agent.core.config.SyncConfig;
import com.jd.live.agent.core.event.FileEvent;
import com.jd.live.agent.core.event.Publisher;
import com.jd.live.agent.core.extension.annotation.ConditionalOnProperty;
import com.jd.live.agent.core.extension.annotation.Extension;
import com.jd.live.agent.core.inject.annotation.Config;
import com.jd.live.agent.core.inject.annotation.Inject;
import com.jd.live.agent.core.inject.annotation.Injectable;
import com.jd.live.agent.core.parser.ObjectParser;
import com.jd.live.agent.core.parser.TypeReference;
import com.jd.live.agent.core.service.AbstractFileSyncer;
import com.jd.live.agent.governance.config.GovernanceConfig;
import com.jd.live.agent.governance.policy.GovernancePolicy;
import com.jd.live.agent.governance.policy.PolicySupervisor;
import com.jd.live.agent.governance.policy.live.LiveSpace;

import java.io.InputStreamReader;
import java.util.List;

/**
 * LiveSpaceFileSyncer
 *
 * @since 1.0.0
 */
@Injectable
@Extension("LiveSpaceFileSyncer")
@ConditionalOnProperty(name = SyncConfig.SYNC_LIVE_SPACE_TYPE, value = "file")
@ConditionalOnProperty(name = GovernanceConfig.CONFIG_LIVE_ENABLED, matchIfMissing = true)
public class LiveSpaceFileSyncer extends AbstractFileSyncer<List<LiveSpace>> {

    private static final Logger logger = LoggerFactory.getLogger(LiveSpaceFileSyncer.class);

    private static final String CONFIG_LIVE_SPACE = "livespaces.json";

    @Inject(PolicySupervisor.COMPONENT_POLICY_SUPERVISOR)
    private PolicySupervisor policySupervisor;

    @Config(SyncConfig.SYNC_LIVE_SPACE)
    private SyncConfig syncConfig = new SyncConfig();

    public LiveSpaceFileSyncer() {
    }

    public LiveSpaceFileSyncer(PolicySupervisor policySupervisor, ObjectParser jsonParser,
                               Publisher<FileEvent> publisher) {
        this.policySupervisor = policySupervisor;
        this.jsonParser = jsonParser;
        this.publisher = publisher;
    }

    @Override
    protected String getName() {
        return "live-syncer";
    }

    @Override
    protected List<LiveSpace> parse(InputStreamReader reader) {
        return jsonParser.read(reader, new TypeReference<List<LiveSpace>>() {
        });
    }

    @Override
    protected SyncConfig getSyncConfig() {
        return syncConfig;
    }

    @Override
    protected String getResource(SyncConfig config) {
        String result = super.getResource(config);
        return result == null || result.isEmpty() ? CONFIG_LIVE_SPACE : result;
    }

    @Override
    protected boolean updateOnce(List<LiveSpace> value, FileDigest meta) {
        GovernancePolicy expect = policySupervisor.getPolicy();
        GovernancePolicy update = expect == null ? new GovernancePolicy() : expect.copy();
        update.setLiveSpaces(value);
        update.cache();
        if (policySupervisor.update(expect, update)) {
            logger.info("success synchronizing file " + file.getPath());
            return true;
        }
        return false;
    }

}
