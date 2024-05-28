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

import com.jd.live.agent.bootstrap.logger.Logger;
import com.jd.live.agent.bootstrap.logger.LoggerFactory;
import com.jd.live.agent.core.config.SyncConfig;
import com.jd.live.agent.core.extension.ExtensionInitializer;
import com.jd.live.agent.core.extension.annotation.ConditionalOnProperty;
import com.jd.live.agent.core.extension.annotation.Extension;
import com.jd.live.agent.core.inject.annotation.Config;
import com.jd.live.agent.core.inject.annotation.Inject;
import com.jd.live.agent.core.inject.annotation.Injectable;
import com.jd.live.agent.core.instance.Application;
import com.jd.live.agent.core.instance.Location;
import com.jd.live.agent.core.parser.ObjectParser;
import com.jd.live.agent.core.parser.TypeReference;
import com.jd.live.agent.core.service.AbstractSyncer;
import com.jd.live.agent.core.service.SyncResult;
import com.jd.live.agent.core.util.http.HttpResponse;
import com.jd.live.agent.core.util.http.HttpState;
import com.jd.live.agent.core.util.http.HttpStatus;
import com.jd.live.agent.core.util.http.HttpUtils;
import com.jd.live.agent.core.util.template.Template;
import com.jd.live.agent.governance.config.GovernanceConfig;
import com.jd.live.agent.governance.policy.GovernancePolicy;
import com.jd.live.agent.governance.policy.PolicySupervisor;
import com.jd.live.agent.governance.policy.live.LiveSpace;
import com.jd.live.agent.implement.service.policy.multilive.config.LiveSyncConfig;
import com.jd.live.agent.implement.service.policy.multilive.reponse.Error;
import com.jd.live.agent.implement.service.policy.multilive.reponse.Response;
import com.jd.live.agent.implement.service.policy.multilive.reponse.Workspace;

import java.io.IOException;
import java.io.SyncFailedException;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * LiveSpaceSyncer is responsible for synchronizing live spaces from a multilive environment.
 */
@Injectable
@Extension("LiveSpaceSyncer")
@ConditionalOnProperty(name = SyncConfig.SYNC_LIVE_SPACE_TYPE, value = "multilive")
@ConditionalOnProperty(value = GovernanceConfig.CONFIG_LIVE_ENABLED, matchIfMissing = true)
public class LiveSpaceSyncer extends AbstractSyncer<List<LiveSpace>, Map<String, Long>> implements ExtensionInitializer {

    private static final Logger logger = LoggerFactory.getLogger(LiveSpaceSyncer.class);

    private static final String SPACE_ID = "space_id";

    private static final String SPACE_VERSION = "space_version";

    @Inject(PolicySupervisor.COMPONENT_POLICY_SUPERVISOR)
    private PolicySupervisor policySupervisor;

    @Inject(Application.COMPONENT_APPLICATION)
    private Application application;

    @Config(SyncConfig.SYNC_LIVE_SPACE)
    private LiveSyncConfig syncConfig = new LiveSyncConfig();

    @Inject(ObjectParser.JSON)
    private ObjectParser jsonParser;

    private Template template;

    @Override
    public void initialize() {
        template = new Template(syncConfig.getSpaceUrl());
    }

    @Override
    protected String getName() {
        return "live-space-syncer";
    }

    @Override
    protected SyncConfig getSyncConfig() {
        return syncConfig;
    }

    @Override
    protected boolean updateOnce(List<LiveSpace> value, Map<String, Long> meta) {
        return policySupervisor.update(policy -> newPolicy(value, meta, policy));
    }

    @Override
    protected void onUpdated() {
        logger.info(getSuccessMessage());
    }

    @Override
    protected void onNotModified() {
        if (shouldPrint()) {
            logger.info(getSuccessMessage());
        }
    }

    @Override
    protected void onFailed(Throwable throwable) {
        logger.error(getErrorMessage(throwable), throwable);
    }

    @Override
    protected SyncResult<List<LiveSpace>, Map<String, Long>> doSynchronize(SyncConfig config, Map<String, Long> last) throws Exception {
        Location location = application.getLocation();
        String liveSpaceId = location == null ? null : location.getLiveSpaceId();
        if (liveSpaceId == null) {
            return syncSpaces(config, last);
        } else {
            return syncSpace(liveSpaceId, config, last);
        }
    }

    /**
     * Synchronizes a specific live space.
     *
     * @param liveSpaceId the ID of the live space.
     * @param version the version of the live space.
     * @param config the synchronization configuration.
     * @param versions the map of versions.
     * @return the synchronized live space.
     * @throws IOException if an I/O error occurs.
     */
    private LiveSpace syncSpace(String liveSpaceId, long version, SyncConfig config, Map<String, Long> versions) throws IOException {
        Response<LiveSpace> response = getSpace(liveSpaceId, version, (LiveSyncConfig) config);
        HttpStatus status = response.getStatus();
        switch (status) {
            case OK:
                LiveSpace space = response.getData();
                versions.put(liveSpaceId, space.getSpec().getVersion());
                return space;
            case NOT_MODIFIED:
                versions.put(liveSpaceId, version);
                return null;
            default:
                versions.remove(liveSpaceId);
                return null;
        }
    }

    /**
     * Synchronizes a specific live space and returns the result.
     *
     * @param liveSpaceId the ID of the live space.
     * @param config the synchronization configuration.
     * @param last the last synchronization metadata.
     * @return the result of the synchronization.
     * @throws IOException if an I/O error occurs.
     */
    private SyncResult<List<LiveSpace>, Map<String, Long>> syncSpace(String liveSpaceId, SyncConfig config, Map<String, Long> last) throws IOException {
        List<LiveSpace> liveSpaces = new ArrayList<>();
        Long version = last == null ? null : last.get(liveSpaceId);
        version = version == null ? 0 : version;
        Map<String, Long> versions = new HashMap<>();
        LiveSpace space = syncSpace(liveSpaceId, version, config, versions);
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
     * Synchronizes all live spaces and returns the result.
     *
     * @param config the synchronization configuration.
     * @param last the last synchronization metadata.
     * @return the result of the synchronization.
     * @throws IOException if an I/O error occurs.
     */
    private SyncResult<List<LiveSpace>, Map<String, Long>> syncSpaces(SyncConfig config, Map<String, Long> last) throws IOException {
        List<LiveSpace> liveSpaces = new ArrayList<>();
        Map<String, Long> spaces = getSpaces((LiveSyncConfig) config);
        for (Map.Entry<String, Long> entry : spaces.entrySet()) {
            String liveSpaceId = entry.getKey();
            Long version = entry.getValue() == null ? 0L : entry.getValue();
            Long lastVersion = last == null ? 0L : last.getOrDefault(liveSpaceId, 0L);
            if (!version.equals(lastVersion)) {
                LiveSpace liveSpace = syncSpace(liveSpaceId, lastVersion, config, spaces);
                if (liveSpace != null) {
                    liveSpaces.add(liveSpace);
                }
            }
        }
        return spaces.equals(last) ? null : new SyncResult<>(liveSpaces, spaces);
    }

    /**
     * Creates a new policy based on the given live spaces and metadata.
     *
     * @param liveSpaces the list of live spaces.
     * @param meta the metadata map.
     * @param old the old policy.
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
     * @throws IOException if an I/O error occurs.
     */
    private Map<String, Long> getSpaces(LiveSyncConfig config) throws IOException {
        String uri = config.getSpacesUrl();
        HttpResponse<Response<List<Workspace>>> httpResponse = HttpUtils.get(uri,
                conn -> configure(config, conn),
                reader -> jsonParser.read(reader, new TypeReference<Response<List<Workspace>>>() {
                }));
        if (httpResponse.getStatus() == HttpStatus.OK) {
            Response<List<Workspace>> response = httpResponse.getData();
            Error error = response.getError();
            if (error == null) {
                List<Workspace> workspaces = response.getData();
                Map<String, Long> map = new HashMap<>();
                if (workspaces != null && !workspaces.isEmpty()) {
                    for (Workspace workspace : workspaces) {
                        map.put(workspace.getId(), workspace.getVersion());
                    }
                }
                return map;
            }
            throw new SyncFailedException(getErrorMessage(error));
        }
        throw new SyncFailedException(getErrorMessage(httpResponse));
    }

    /**
     * Retrieves the live space information from the remote server.
     *
     * @param workspaceId the ID of the workspace to retrieve.
     * @param version the version of the workspace to retrieve.
     * @param config the synchronization configuration.
     * @return the response containing the live space information.
     * @throws IOException if an I/O error occurs during the HTTP request.
     */
    private Response<LiveSpace> getSpace(String workspaceId, Long version, LiveSyncConfig config) throws IOException {
        Map<String, Object> context = new HashMap<>(2);
        context.put(SPACE_ID, workspaceId);
        context.put(SPACE_VERSION, version);
        String uri = template.evaluate(context);
        HttpResponse<Response<LiveSpace>> httpResponse = HttpUtils.get(uri,
                conn -> configure(config, conn),
                reader -> jsonParser.read(reader, new TypeReference<Response<LiveSpace>>() {
                }));
        if (httpResponse.getStatus() == HttpStatus.OK) {
            Response<LiveSpace> response = httpResponse.getData();
            Error error = response.getError();
            HttpStatus status = response.getStatus();
            switch (status) {
                case OK:
                case NOT_MODIFIED:
                case NOT_FOUND:
                    return response;
            }
            throw new SyncFailedException(getErrorMessage(error, workspaceId));
        }
        throw new SyncFailedException(getErrorMessage(httpResponse, workspaceId));
    }

    /**
     * Configures the HTTP connection with the necessary headers and timeout settings.
     *
     * @param config the synchronization configuration.
     * @param conn the HTTP connection to configure.
     */
    private void configure(SyncConfig config, HttpURLConnection conn) {
        config.header(conn::setRequestProperty);
        conn.setRequestProperty("Accept", "application/json");
        conn.setConnectTimeout((int) config.getTimeout());
    }

    /**
     * Constructs a success message for logging purposes.
     *
     * @return the success message.
     */
    private String getSuccessMessage() {
        return "Success synchronizing live space policy from multilive. counter=" + counter.get();
    }

    /**
     * Constructs an error message for logging purposes based on the HTTP state.
     *
     * @param reply the HTTP state containing the error information.
     * @return the error message.
     */
    private String getErrorMessage(HttpState reply) {
        return "Failed to synchronize live space from multilive. code=" + reply.getCode()
                + ", message=" + reply.getMessage()
                + ", counter=" + counter.get();
    }

    /**
     * Constructs an error message for logging purposes based on the HTTP state and workspace ID.
     *
     * @param reply the HTTP state containing the error information.
     * @param workspaceId the ID of the workspace that failed to synchronize.
     * @return the error message.
     */
    private String getErrorMessage(HttpState reply, String workspaceId) {
        return "Failed to synchronize live space from multilive. space=" + workspaceId
                + ", code=" + reply.getCode()
                + ", message=" + reply.getMessage()
                + ", counter=" + counter.get();
    }

    /**
     * Constructs an error message for logging purposes based on an exception.
     *
     * @param throwable the exception that caused the synchronization failure.
     * @return the error message.
     */
    private String getErrorMessage(Throwable throwable) {
        return "Failed to synchronize live space from multilive. counter=" + counter.get() + ", caused by " + throwable.getMessage();
    }
}
