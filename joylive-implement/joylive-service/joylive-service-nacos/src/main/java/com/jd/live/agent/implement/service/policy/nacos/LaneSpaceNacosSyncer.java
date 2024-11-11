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

import com.alibaba.nacos.api.config.listener.Listener;
import com.jd.live.agent.bootstrap.logger.Logger;
import com.jd.live.agent.bootstrap.logger.LoggerFactory;
import com.jd.live.agent.core.config.SyncConfig;
import com.jd.live.agent.core.event.Publisher;
import com.jd.live.agent.core.extension.annotation.ConditionalOnProperty;
import com.jd.live.agent.core.extension.annotation.Extension;
import com.jd.live.agent.core.inject.annotation.Config;
import com.jd.live.agent.core.inject.annotation.Inject;
import com.jd.live.agent.core.inject.annotation.Injectable;
import com.jd.live.agent.core.instance.Application;
import com.jd.live.agent.core.parser.ObjectParser;
import com.jd.live.agent.core.parser.TypeReference;
import com.jd.live.agent.core.thread.NamedThreadFactory;
import com.jd.live.agent.core.util.Close;
import com.jd.live.agent.core.util.time.Timer;
import com.jd.live.agent.governance.config.GovernanceConfig;
import com.jd.live.agent.governance.policy.GovernancePolicy;
import com.jd.live.agent.governance.policy.PolicySubscriber;
import com.jd.live.agent.governance.policy.PolicySupervisor;
import com.jd.live.agent.governance.policy.lane.LaneSpace;
import com.jd.live.agent.governance.service.PolicyService;
import com.jd.live.agent.implement.service.policy.nacos.config.NacosSyncConfig;

import java.io.StringReader;
import java.util.List;
import java.util.concurrent.*;

/**
 * LaneSpaceNacosSyncer is responsible for synchronizing lane spaces policies from nacos.
 */
@Injectable
@Extension("LaneSpaceNacosSyncer")
@ConditionalOnProperty(name = SyncConfig.SYNC_LIVE_SPACE_TYPE, value = "nacos")
@ConditionalOnProperty(name = SyncConfig.SYNC_LIVE_SPACE_SERVICE, matchIfMissing = true)
@ConditionalOnProperty(name = GovernanceConfig.CONFIG_LIVE_ENABLED, matchIfMissing = true)
public class LaneSpaceNacosSyncer extends AbstractNacosSyncer implements PolicyService {

    private static final Logger logger = LoggerFactory.getLogger(ServiceNacosSyncer.class);
    private static final String LANE_SPACE_DATA_ID = "lanes.json";

    @Inject(PolicySupervisor.COMPONENT_POLICY_SUPERVISOR)
    private PolicySupervisor policySupervisor;

    private static final int CONCURRENCY = 3;

    @Inject(Application.COMPONENT_APPLICATION)
    private Application application;

    @Inject(Timer.COMPONENT_TIMER)
    private Timer timer;

    @Inject(Publisher.POLICY_SUBSCRIBER)
    private Publisher<PolicySubscriber> publisher;

    @Inject(ObjectParser.JSON)
    private ObjectParser jsonParser;

    @Config(SyncConfig.SYNC_LIVE_SPACE)
    private NacosSyncConfig syncConfig = new NacosSyncConfig();

    private ExecutorService executorService;
    /**
     * catch nacos listeners
     */
    private Listener listener;

    @Override
    protected CompletableFuture<Void> doStart() {
        int concurrency = syncConfig.getConcurrency() <= 0 ? CONCURRENCY : syncConfig.getConcurrency();
        executorService = Executors.newFixedThreadPool(concurrency, new NamedThreadFactory(getName(), true));
        super.doStart();
        syncAndUpdate();
        return CompletableFuture.completedFuture(null);
    }

    @Override
    protected CompletableFuture<Void> doStop() {
        Close.instance().closeIfExists(executorService, ExecutorService::shutdownNow);
        return super.doStop();
    }

    /**
     * Synchronizes and updates the LanSpaces
     */
    private void syncAndUpdate() {
        try {
            //first: get config
            String config = getConfigService().getConfig(LANE_SPACE_DATA_ID, syncConfig.getLaneSpaceNacosGroup(), syncConfig.getTimeout());
            List<LaneSpace> laneSpaces = parseLaneSpaces(config);
            // then: add listener
            if (listener == null) {
                Listener listener = new Listener() {
                    @Override
                    public Executor getExecutor() {
                        return executorService;
                    }

                    @Override
                    public void receiveConfigInfo(String configInfo) {
                        syncAndUpdate();
                    }
                };
                getConfigService().addListener(LANE_SPACE_DATA_ID, syncConfig.getServiceNacosGroup(), listener);
                this.listener = listener;
            }

            for (int i = 0; i < UPDATE_MAX_RETRY; i++) {
                if (updateOnce(laneSpaces)) {
                    return;
                }
            }
        } catch (Throwable t) {
            logger.error("syn lane space failed", t);
            throw new RuntimeException(t);
        }

    }

    protected List<LaneSpace> parseLaneSpaces(String configInfo) {
        try (StringReader reader = new StringReader(configInfo)) {
            return jsonParser.read(reader, new TypeReference<List<LaneSpace>>() {
            });
        }
    }

    /**
     * Performs synchronization and updates the state once.
     *
     * @return True if the synchronization and update were successful, false otherwise.
     */
    protected boolean updateOnce(List<LaneSpace> laneSpaces) {
        if (policySupervisor.update(policy -> newPolicy(policy, laneSpaces))) {
            logger.info("Success synchronizing");
            return true;
        }
        return false;
    }

    /**
     * Creates a new policy based on the given service.
     *
     * @param policy policy
     * @param laneSpaces lane spaces
     * @return the new policy.
     */
    private GovernancePolicy newPolicy(GovernancePolicy policy, List<LaneSpace> laneSpaces) {
        GovernancePolicy result = policy == null ? new GovernancePolicy() : policy.copy();
        result.setLaneSpaces(laneSpaces);
        return result;
    }

    @Override
    public PolicyType getPolicyType() {
        return PolicyType.LANE_SPACE;
    }

    @Override
    protected NacosSyncConfig getSyncConfig() {
        return syncConfig;
    }
}
