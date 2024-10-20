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
import com.jd.live.agent.bootstrap.exception.InitializeException;
import com.jd.live.agent.bootstrap.logger.Logger;
import com.jd.live.agent.bootstrap.logger.LoggerFactory;
import com.jd.live.agent.core.config.SyncConfig;
import com.jd.live.agent.core.extension.annotation.ConditionalOnProperty;
import com.jd.live.agent.core.extension.annotation.Extension;
import com.jd.live.agent.core.inject.annotation.Config;
import com.jd.live.agent.core.inject.annotation.Inject;
import com.jd.live.agent.core.inject.annotation.Injectable;
import com.jd.live.agent.core.instance.Application;
import com.jd.live.agent.core.instance.Location;
import com.jd.live.agent.core.parser.ObjectParser;
import com.jd.live.agent.core.parser.TypeReference;
import com.jd.live.agent.core.parser.json.JsonAlias;
import com.jd.live.agent.core.service.SyncResult;
import com.jd.live.agent.core.thread.NamedThreadFactory;
import com.jd.live.agent.core.util.Close;
import com.jd.live.agent.governance.config.GovernanceConfig;
import com.jd.live.agent.governance.policy.GovernancePolicy;
import com.jd.live.agent.governance.policy.PolicySupervisor;
import com.jd.live.agent.governance.policy.PolicyType;
import com.jd.live.agent.governance.policy.live.LiveSpace;
import com.jd.live.agent.governance.service.PolicyService;
import com.jd.live.agent.implement.service.policy.nacos.config.NacosSyncConfig;
import lombok.Getter;
import lombok.Setter;

import java.io.IOException;
import java.io.StringReader;
import java.io.SyncFailedException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * LiveSpaceSyncer is responsible for synchronizing live spaces from nacos.
 */
@Injectable
@Extension("LiveSpaceNacosSyncer")
@ConditionalOnProperty(name = SyncConfig.SYNC_LIVE_SPACE_TYPE, value = "nacos")
@ConditionalOnProperty(name = SyncConfig.SYNC_LIVE_SPACE_SERVICE, matchIfMissing = true)
@ConditionalOnProperty(value = GovernanceConfig.CONFIG_LIVE_ENABLED, matchIfMissing = true)
public class LiveSpaceNacosSyncer extends AbstractNacosSyncer implements PolicyService {

    private static final Logger logger = LoggerFactory.getLogger(LiveSpaceNacosSyncer.class);

    private static final String WORKSPACES_DATA_ID = "workspaces.json";

    private static final int CONCURRENCY = 3;

    @Inject(PolicySupervisor.COMPONENT_POLICY_SUPERVISOR)
    private PolicySupervisor policySupervisor;

    @Inject(Application.COMPONENT_APPLICATION)
    private Application application;

    @Config(SyncConfig.SYNC_LIVE_SPACE)
    private NacosSyncConfig syncConfig = new NacosSyncConfig();

    @Inject(ObjectParser.JSON)
    private ObjectParser jsonParser;

    protected final AtomicLong counter = new AtomicLong(0);
    /**
     * The last metadata object obtained from the synchronization process.
     */
    protected Map<String, Long> last;

    private ExecutorService executorService;
    /**
     * catch nacos listeners
     */
    private final Map<String, Listener> listeners = new ConcurrentHashMap<>();

    @Override
    protected CompletableFuture<Void> doStart() {
        int concurrency = syncConfig.getConcurrency() <= 0 ? CONCURRENCY : syncConfig.getConcurrency();
        executorService = Executors.newFixedThreadPool(concurrency, new NamedThreadFactory(getName(), true));
        super.doStart();
        try {
            syncAndUpdate();
        } catch (Exception e) {
            logger.error("start LiveSpaceNacosSyncer failed " + e.getMessage(), e);
            throw new InitializeException("start LiveSpaceNacosSyncer failed " + e.getMessage(), e);
        }
        return CompletableFuture.completedFuture(null);
    }

    @Override
    protected CompletableFuture<Void> doStop() {
        Close.instance().closeIfExists(executorService, ExecutorService::shutdownNow);
        return super.doStop();
    }

    /**
     * Performs synchronization and updates the state once.
     *
     * @return True if the synchronization and update were successful, false otherwise.
     * @throws Exception If an error occurs during synchronization.
     */
    protected boolean syncAndUpdate() throws Exception {
        SyncResult<List<LiveSpace>, Map<String, Long>> result = doSynchronize(syncConfig, last);
        if (result != null) {
            last = result.getMeta();
            for (int i = 0; i < UPDATE_MAX_RETRY; i++) {
                if (updateOnce(result.getData(), result.getMeta())) {
                    onUpdated();
                    return true;
                }
            }
        } else {
            onNotModified();
        }
        return false;
    }

    protected SyncResult<List<LiveSpace>, Map<String, Long>> doSynchronize(SyncConfig config, Map<String, Long> last) throws Exception {
        Location location = application.getLocation();
        String liveSpaceId = location == null ? null : location.getLiveSpaceId();
        if (liveSpaceId == null) {
            return syncSpaces(last);
        } else {
            return syncSpace(liveSpaceId, last);
        }
    }

    /**
     * Synchronizes all live spaces and returns the result.
     *
     * @param last the last synchronization metadata.
     * @return the result of the synchronization.
     * @throws IOException if an I/O error occurs.
     */
    private SyncResult<List<LiveSpace>, Map<String, Long>> syncSpaces(Map<String, Long> last) throws IOException {
        List<LiveSpace> liveSpaces = new ArrayList<>();
        Map<String, Long> spaces = getSpaces(syncConfig);
        for (Map.Entry<String, Long> entry : spaces.entrySet()) {
            String liveSpaceId = entry.getKey();
            Long version = entry.getValue() == null ? 0L : entry.getValue();
            Long lastVersion = last == null ? 0L : last.getOrDefault(liveSpaceId, 0L);
            if (!version.equals(lastVersion)) {
                LiveSpace liveSpace = syncSpace(liveSpaceId, lastVersion, spaces);
                if (liveSpace != null) {
                    liveSpaces.add(liveSpace);
                }
            }
        }
        return spaces.equals(last) ? null : new SyncResult<>(liveSpaces, spaces);
    }


    /**
     * Updates the state with the new data and metadata.
     *
     * @param value The new data to update.
     * @param meta  The new metadata to update.
     * @return True if the update was successful, false otherwise.
     */
    private boolean updateOnce(List<LiveSpace> value, Map<String, Long> meta) {
        return policySupervisor.update(policy -> newPolicy(value, meta, policy));
    }


    /**
     * Synchronizes a specific live space.
     *
     * @param liveSpaceId the ID of the live space.
     * @param version     the version of the live space.
     * @param versions    the map of versions.
     * @return the synchronized live space.
     * @throws SyncFailedException exeption
     */
    private LiveSpace syncSpace(String liveSpaceId, long version, Map<String, Long> versions) throws SyncFailedException {
        LiveSpace space = getSpace(liveSpaceId, version);
        versions.put(liveSpaceId, space.getSpec().getVersion());
        return space;
    }

    /**
     * Synchronizes a specific live space and returns the result.
     *
     * @param liveSpaceId the ID of the live space.
     * @param last        the last synchronization metadata.
     * @return the result of the synchronization.
     * @throws IOException if an I/O error occurs.
     */
    private SyncResult<List<LiveSpace>, Map<String, Long>> syncSpace(String liveSpaceId, Map<String, Long> last) throws IOException {
        List<LiveSpace> liveSpaces = new ArrayList<>();
        Long version = last == null ? null : last.get(liveSpaceId);
        version = version == null ? 0 : version;
        Map<String, Long> versions = new HashMap<>();
        LiveSpace space = syncSpace(liveSpaceId, version, versions);
        if (space != null) {
            liveSpaces.add(space);
            return new SyncResult<>(liveSpaces, versions);
        } else if (versions.equals(last)) {
            return null;
        } else {
            return new SyncResult<>(liveSpaces, versions);
        }
    }


    /**
     * Creates a new policy based on the given live spaces and metadata.
     *
     * @param liveSpaces the list of live spaces.
     * @param meta       the metadata map.
     * @param old        the old policy.
     * @return the new policy.
     */
    private GovernancePolicy newPolicy(List<LiveSpace> liveSpaces, Map<String, Long> meta, GovernancePolicy old) {
        List<LiveSpace> oldSpaces = old == null ? null : old.getLiveSpaces();
        if (oldSpaces != null) {
            Long version;
            for (LiveSpace space : oldSpaces) {
                version = meta.get(space.getId());
                if (version != null && version.equals(space.getSpec().getVersion())) {
                    liveSpaces.add(space);
                }
            }
        }
        GovernancePolicy result = old == null ? new GovernancePolicy() : old.copy();
        result.setLiveSpaces(liveSpaces);
        return result;
    }

    /**
     * Retrieves the map of live spaces and their versions from the remote server.
     *
     * @param config the synchronization configuration.
     * @return the map of live spaces and their versions.
     * @throws Exception exception
     */
    private Map<String, Long> getSpaces(NacosSyncConfig config) throws SyncFailedException {
        try {
            //first: get config
            String configInfo = getConfigService().getConfig(WORKSPACES_DATA_ID,
                    syncConfig.getLiveSpaceNacosGroup(),
                    syncConfig.getTimeout());
            // then: add listener
            if (listeners.get(WORKSPACES_DATA_ID) == null) {
                Listener listener = new Listener() {
                    @Override
                    public Executor getExecutor() {
                        return executorService;
                    }

                    @Override
                    public void receiveConfigInfo(String configInfo) {
                        try {
                            syncAndUpdate();
                        } catch (Exception e) {
                            logger.error(getErrorMessage(e));
                        }
                    }
                };
                getConfigService().addListener(WORKSPACES_DATA_ID, syncConfig.getLiveSpaceNacosGroup(), listener);
                listeners.put(WORKSPACES_DATA_ID, listener);
            }

            List<Workspace> workspaces = parseWrokspaces(configInfo);
            Map<String, Long> map = new HashMap<>();
            if (workspaces != null && !workspaces.isEmpty()) {
                for (Workspace workspace : workspaces) {
                    map.put(workspace.getId(), workspace.getVersion());
                }
            }
            return map;
        } catch (Throwable t) {
            throw new SyncFailedException(getErrorMessage(t));
        }
    }


    private List<Workspace> parseWrokspaces(String configInfo) {
        StringReader reader = new StringReader(configInfo);
        List<Workspace> workspaces = jsonParser.read(reader, new TypeReference<List<Workspace>>() {
        });
        reader.close();
        return workspaces;
    }

    private LiveSpace parseLivespace(String configInfo) {
        StringReader reader = new StringReader(configInfo);
        LiveSpace liveSpace = jsonParser.read(reader, new TypeReference<LiveSpace>() {
        });
        reader.close();
        return liveSpace;
    }

    /**
     * Retrieves the live space information from the remote server.
     *
     * @param workspaceId the ID of the workspace to retrieve.
     * @param version     the version of the workspace to retrieve.
     * @return the response containing the live space information.
     * @throws SyncFailedException if an I/O error occurs during the HTTP request.
     */
    private LiveSpace getSpace(String workspaceId, Long version) throws SyncFailedException {
        try {
            //first: get config
            String configInfo = getConfigService().getConfig(workspaceId,
                    syncConfig.getLiveSpaceNacosGroup(),
                    syncConfig.getTimeout());
            // then: add listener
            if (listeners.get(workspaceId) == null) {
                Listener listener = new Listener() {
                    @Override
                    public Executor getExecutor() {
                        return executorService;
                    }

                    @Override
                    public void receiveConfigInfo(String configInfo) {
                        try {
                            syncAndUpdate();
                        } catch (Exception e) {
                            logger.error(getErrorMessage(e));
                        }
                    }
                };
                getConfigService().addListener(workspaceId, syncConfig.getLiveSpaceNacosGroup(), listener);
                listeners.put(workspaceId, listener);
            }

            LiveSpace liveSpace = parseLivespace(configInfo);
            return liveSpace;
        } catch (Throwable t) {
            throw new SyncFailedException(getErrorMessage(t));
        }
    }


    protected void onUpdated() {
        logger.info(getSuccessMessage());
    }

    protected void onNotModified() {
        logger.info(getSuccessMessage());
    }

    /**
     * Constructs a success message for logging purposes.
     *
     * @return the success message.
     */
    private String getSuccessMessage() {
        return "Success synchronizing live space policy from nacos. counter=" + counter.get();
    }

    /**
     * Constructs an error message for logging purposes based on an exception.
     *
     * @param throwable the exception that caused the synchronization failure.
     * @return the error message.
     */
    private String getErrorMessage(Throwable throwable) {
        return "Failed to synchronize live space from nacos. counter=" + counter.get() + ", caused by " + throwable.getMessage();
    }

    @Override
    public PolicyType getPolicyType() {
        return PolicyType.LIVE_SPACE;
    }


    @Override
    protected NacosSyncConfig getSyncConfig() {
        return syncConfig;
    }

    @Getter
    @Setter
    public static class Workspace {

        @JsonAlias("workspaceId")
        private String id;

        private String code;

        private String name;

        private Long version;
    }
}
