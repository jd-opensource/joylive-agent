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
package com.jd.live.agent.governance.service.sync;

import com.jd.live.agent.bootstrap.logger.Logger;
import com.jd.live.agent.bootstrap.logger.LoggerFactory;
import com.jd.live.agent.core.config.ConfigWatcher;
import com.jd.live.agent.core.config.SyncConfig;
import com.jd.live.agent.core.inject.annotation.Inject;
import com.jd.live.agent.core.instance.Application;
import com.jd.live.agent.core.instance.Location;
import com.jd.live.agent.core.parser.ObjectParser;
import com.jd.live.agent.core.parser.TypeReference;
import com.jd.live.agent.core.service.sync.AbstractConfigSyncer;
import com.jd.live.agent.core.service.sync.SyncResult;
import com.jd.live.agent.core.util.http.HttpResponse;
import com.jd.live.agent.core.util.http.HttpState;
import com.jd.live.agent.core.util.http.HttpStatus;
import com.jd.live.agent.core.util.http.HttpUtils;
import com.jd.live.agent.core.util.template.Template;
import com.jd.live.agent.governance.policy.live.LiveSpace;
import com.jd.live.agent.governance.service.sync.api.ApiError;
import com.jd.live.agent.governance.service.sync.api.ApiResponse;
import com.jd.live.agent.governance.service.sync.api.ApiSpace;

import java.io.IOException;
import java.io.SyncFailedException;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * An abstract class that provides a base implementation for synchronizing live spaces with an HTTP service.
 */
public abstract class AbstractLiveSpaceHttpSyncer extends AbstractConfigSyncer<List<LiveSpace>, Map<String, Long>> {

    private static final Logger logger = LoggerFactory.getLogger(AbstractLiveSpaceHttpSyncer.class);

    protected static final String SPACE_ID = "space_id";

    protected static final String SPACE_VERSION = "space_version";

    @Inject(Application.COMPONENT_APPLICATION)
    protected Application application;

    @Inject(ObjectParser.JSON)
    protected ObjectParser parser;

    protected Template template;

    @Override
    protected CompletableFuture<Void> doStart() {
        template = new Template(((SyncAddress.LiveSpaceAddress) getSyncConfig()).getSpaceUrl());
        return super.doStart();
    }

    @Override
    protected SyncResult<List<LiveSpace>, Map<String, Long>> doSynchronize(Map<String, Long> last) throws Exception {
        Location location = application.getLocation();
        String liveSpaceId = location == null ? null : location.getLiveSpaceId();
        if (liveSpaceId == null) {
            return syncSpaces(last);
        } else {
            return syncSpace(liveSpaceId, last);
        }
    }

    @Override
    public String getType() {
        return ConfigWatcher.TYPE_LIVE_SPACE;
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

    /**
     * Synchronizes a specific live space.
     *
     * @param liveSpaceId the ID of the live space.
     * @param version     the version of the live space.
     * @param versions    the map of versions.
     * @return the synchronized live space.
     * @throws IOException if an I/O error occurs.
     */
    protected LiveSpace syncSpace(String liveSpaceId, long version, Map<String, Long> versions) throws IOException {
        ApiResponse<LiveSpace> response = getSpace(liveSpaceId, version);
        HttpStatus status = response.getStatus();
        switch (status) {
            case OK:
                LiveSpace space = response.getData();
                versions.put(liveSpaceId, space.getSpec().getVersion());
                return space;
            case NOT_FOUND:
                versions.remove(liveSpaceId);
                return null;
            case NOT_MODIFIED:
            default:
                versions.put(liveSpaceId, version);
                return null;
        }
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
     * Synchronizes all live spaces and returns the result.
     *
     * @param last the last synchronization metadata.
     * @return the result of the synchronization.
     * @throws IOException if an I/O error occurs.
     */
    protected SyncResult<List<LiveSpace>, Map<String, Long>> syncSpaces(Map<String, Long> last) throws IOException {
        List<LiveSpace> liveSpaces = new ArrayList<>();
        Map<String, Long> spaces = getSpaces();
        for (Map.Entry<String, Long> entry : spaces.entrySet()) {
            String liveSpaceId = entry.getKey();
            Long version = entry.getValue() == null ? 0L : entry.getValue();
            long lastVersion = last == null ? 0L : last.getOrDefault(liveSpaceId, 0L);
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
     * Retrieves the map of live spaces and their versions from the remote server.
     *
     * @return the map of live spaces and their versions.
     * @throws IOException if an I/O error occurs.
     */
    protected Map<String, Long> getSpaces() throws IOException {
        HttpResponse<ApiResponse<List<ApiSpace>>> response = requestSpaces();
        if (HttpStatus.OK == response.getStatus()) {
            ApiResponse<List<ApiSpace>> apiResponse = response.getData();
            ApiError error = apiResponse.getError();
            if (error != null) {
                throw new SyncFailedException(getErrorMessage(error));
            }
            List<ApiSpace> spaces = apiResponse.getData();
            Map<String, Long> result = new HashMap<>();
            if (spaces != null && !spaces.isEmpty()) {
                for (ApiSpace space : spaces) {
                    result.put(space.getId(), space.getVersion());
                }
            }
            return result;
        }
        throw new SyncFailedException(getErrorMessage(response));
    }

    /**
     * Sends an HTTP GET request to retrieve a list of live spaces from the specified URI.
     *
     * @return The HTTP response containing the retrieved list of live spaces as an {@link ApiResponse} object.
     * @throws IOException If an I/O error occurs during the request.
     */
    protected HttpResponse<ApiResponse<List<ApiSpace>>> requestSpaces() throws IOException {
        String uri = ((SyncAddress.LiveSpaceAddress) getSyncConfig()).getSpacesUrl();
        return HttpUtils.get(uri, this::configure,
                reader -> parser.read(reader, new TypeReference<ApiResponse<List<ApiSpace>>>() {
                }));
    }

    /**
     * Retrieves the live space information from the remote server.
     *
     * @param spaceId the ID of the workspace to retrieve.
     * @param version the version of the workspace to retrieve.
     * @return the response containing the live space information.
     * @throws IOException if an I/O error occurs during the HTTP request.
     */
    protected ApiResponse<LiveSpace> getSpace(String spaceId, long version) throws IOException {
        HttpResponse<ApiResponse<LiveSpace>> httpResponse = requestSpace(spaceId, version);
        if (httpResponse.getStatus() == HttpStatus.OK) {
            ApiResponse<LiveSpace> response = httpResponse.getData();
            ApiError error = response.getError();
            HttpStatus status = response.getStatus();
            switch (status) {
                case OK:
                case NOT_MODIFIED:
                case NOT_FOUND:
                    return response;
            }
            throw new SyncFailedException(getErrorMessage(error, spaceId));
        }
        throw new SyncFailedException(getErrorMessage(httpResponse, spaceId));
    }

    /**
     * Sends an HTTP GET request to retrieve a live space with the specified workspace ID and version.
     *
     * @param spaceId The ID of the workspace for which to retrieve the live space.
     * @param version The version of the live space to retrieve.
     * @return The HTTP response containing the retrieved live space as an {@link ApiResponse} object.
     * @throws IOException If an I/O error occurs during the request.
     */
    protected HttpResponse<ApiResponse<LiveSpace>> requestSpace(String spaceId, long version) throws IOException {
        Map<String, Object> context = new HashMap<>(2);
        context.put(SPACE_ID, spaceId);
        context.put(SPACE_VERSION, version);
        String uri = template.evaluate(context);
        return HttpUtils.get(uri, this::configure,
                reader -> parser.read(reader, new TypeReference<ApiResponse<LiveSpace>>() {
                }));
    }

    /**
     * Configures the HTTP connection with the necessary headers and timeout settings.
     *
     * @param conn the HTTP connection to configure.
     */
    protected void configure(HttpURLConnection conn) {
        SyncConfig config = getSyncConfig();
        config.header(conn::setRequestProperty);
        application.labelSync(conn::setRequestProperty);
        conn.setRequestProperty("Accept", "application/json");
        conn.setConnectTimeout((int) config.getTimeout());
    }

    /**
     * Constructs a success message for logging purposes.
     *
     * @return the success message.
     */
    protected String getSuccessMessage() {
        return "Success synchronizing live space policy from " + getName() + ". counter=" + counter.get();
    }

    /**
     * Constructs an error message for logging purposes based on the HTTP state.
     *
     * @param reply the HTTP state containing the error information.
     * @return the error message.
     */
    protected String getErrorMessage(HttpState reply) {
        return "Failed to synchronize live space from " + getName() + ". code=" + reply.getCode()
                + ", message=" + reply.getMessage()
                + ", counter=" + counter.get();
    }

    /**
     * Constructs an error message for logging purposes based on the HTTP state and workspace ID.
     *
     * @param reply       the HTTP state containing the error information.
     * @param workspaceId the ID of the workspace that failed to synchronize.
     * @return the error message.
     */
    protected String getErrorMessage(HttpState reply, String workspaceId) {
        return "Failed to synchronize live space from " + getName() + ". space=" + workspaceId
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
    protected String getErrorMessage(Throwable throwable) {
        return "Failed to synchronize live space from " + getName() + ". counter=" + counter.get() + ", caused by " + throwable.getMessage();
    }
}
