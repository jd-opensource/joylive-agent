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

import com.jd.live.agent.core.config.ConfigEvent;
import com.jd.live.agent.core.config.ConfigEvent.EventType;
import com.jd.live.agent.core.config.ConfigWatcher;
import com.jd.live.agent.core.exception.SyncException;
import com.jd.live.agent.core.instance.Location;
import com.jd.live.agent.core.parser.TypeReference;
import com.jd.live.agent.core.util.Close;
import com.jd.live.agent.governance.policy.lane.LaneSpace;
import com.jd.live.agent.governance.service.sync.api.ApiSpace;

import java.io.StringReader;
import java.util.*;

import static com.jd.live.agent.governance.service.sync.SyncKey.LaneSpaceKey;

/**
 * An abstract class that provides a base implementation for synchronizing LaneSpace objects using subscriptions.
 */
public abstract class AbstractLaneSpaceSyncer<K extends LaneSpaceKey> extends AbstractSyncer<K, LaneSpace> {

    protected Syncer<K, List<ApiSpace>> spaceListSyncer;

    protected Subscription<K, List<ApiSpace>> spaceListSubscription;

    @Override
    public String getType() {
        return ConfigWatcher.TYPE_LANE_SPACE;
    }

    @Override
    protected void startSync() throws Exception {
        spaceListSubscription = new Subscription<>(getName(), createSpaceListKey(), this::onSpaceListResponse);
        spaceListSyncer = createSpaceListSyncer();
        Location location = application.getLocation();
        String laneSpaceId = location == null ? null : location.getLaneSpaceId();
        if (laneSpaceId == null) {
            syncSpaceList();
        } else {
            syncSpace(laneSpaceId);
        }
    }

    @Override
    protected void stopSync() {
        Close.instance().closeIfExists(spaceListSyncer, Syncer::close);
        super.stopSync();
    }

    /**
     * Creates a key for synchronizing a list of LaneSpace objects.
     *
     * @return A unique identifier for the list of LaneSpace objects.
     */
    protected abstract K createSpaceListKey();

    /**
     * Creates a key for synchronizing a single LaneSpace object.
     *
     * @param spaceId The ID of the LaneSpace object to synchronize.
     * @return A unique identifier for the specified LaneSpace object.
     */
    protected abstract K createSpaceKey(String spaceId);

    /**
     * Creates a Syncer object for synchronizing a list of ApiSpace objects.
     *
     * @return A Syncer object for synchronizing a list of ApiSpace objects.
     */
    protected abstract Syncer<K, List<ApiSpace>> createSpaceListSyncer();

    /**
     * Synchronizes the list of LaneSpace objects with the remote server.
     */
    protected void syncSpaceList() {
        spaceListSyncer.sync(spaceListSubscription);
    }

    /**
     * Synchronizes a single LaneSpace object with the remote server.
     *
     * @param spaceId The ID of the LaneSpace object to synchronize.
     */
    protected void syncSpace(String spaceId) {
        Subscription<K, LaneSpace> subscription = new Subscription<>(getName(), createSpaceKey(spaceId), r -> onSpaceResponse(r, spaceId));
        if (subscriptions.putIfAbsent(spaceId, subscription) == null) {
            syncer.sync(subscription);
        }
    }

    /**
     * Handles the response from the sync operation for a list of ApiSpace objects.
     *
     * @param response The SyncResponse object containing the response from the sync operation.
     */
    protected void onSpaceListResponse(SyncResponse<List<ApiSpace>> response) {
        switch (response.getStatus()) {
            case SUCCESS:
                updateSpaceList(response.getData());
                break;
            case NOT_MODIFIED:
                break;
            case NOT_FOUND:
                throw new SyncException("Failed to sync lane spaces policy, caused by the spaces is not found.");
            case ERROR:
                throw new SyncException(response.getError());
        }
    }

    /**
     * Handles the response from the sync operation for a LaneSpace object.
     *
     * @param response The SyncResponse object containing the response from the sync operation.
     * @param spaceId The space id.
     */
    protected void onSpaceResponse(SyncResponse<LaneSpace> response, String spaceId) {
        switch (response.getStatus()) {
            case SUCCESS:
                updateSpace(spaceId, response.getData());
                break;
            case NOT_MODIFIED:
                break;
            case NOT_FOUND:
                deleteSpace(spaceId);
                break;
            case ERROR:
                throw new SyncException(response.getError());
        }
    }

    /**
     * Updates the list of LaneSpace objects based on the provided list of ApiSpace objects.
     *
     * @param apiSpaces The list of ApiSpace objects to update the LaneSpace objects from.
     */
    protected void updateSpaceList(List<ApiSpace> apiSpaces) {
        Set<String> spaces = new HashSet<>();
        for (ApiSpace apiSpace : apiSpaces) {
            spaces.add(apiSpace.getId());
            if (!subscriptions.containsKey(apiSpace.getId())) {
                syncSpace(apiSpace.getId());
            }
        }
        for (Map.Entry<String, Subscription<K, LaneSpace>> entry : subscriptions.entrySet()) {
            if (!spaces.contains(entry.getKey())) {
                deleteSpace(entry.getKey());
            }
        }
    }

    /**
     * Updates the specified LaneSpace object.
     *
     * @param spaceId The id of the space.
     * @param space The LaneSpace object to update.
     */
    protected void updateSpace(String spaceId, LaneSpace space) {
        if (space == null) {
            deleteSpace(spaceId);
        } else {
            Subscription<K, LaneSpace> subscription = subscriptions.get(space.getId());
            long version = space.getVersion();
            if (subscription != null) {
                synchronized (subscription) {
                    if (subscription.getVersion() < version) {
                        subscription.setVersion(version);
                        publish(ConfigEvent.builder()
                                .type(EventType.UPDATE_ITEM)
                                .name(space.getId())
                                .value(space)
                                .description("lane space " + space.getId())
                                .watcher(getName())
                                .build());
                    }
                }
            }
        }
    }

    /**
     * Deletes a lane space by its ID.
     *
     * @param spaceId The ID of the lane space to delete.
     */
    protected void deleteSpace(String spaceId) {
        Subscription<K, LaneSpace> subscription = subscriptions.remove(spaceId);
        if (subscription != null) {
            synchronized (subscription) {
                syncer.remove(subscription);
                publish(ConfigEvent.builder()
                        .type(EventType.DELETE_ITEM)
                        .name(spaceId)
                        .description("lane space " + spaceId)
                        .watcher(getName())
                        .build());
            }
        }
    }

    /**
     * Parses a configuration string into a list of ApiSpace objects.
     *
     * @param config The configuration string to parse.
     * @return A list of ApiSpace objects.
     */
    protected SyncResponse<List<ApiSpace>> parseSpaceList(String config) {
        if (config == null || config.isEmpty()) {
            return new SyncResponse<>(SyncStatus.NOT_FOUND, null);
        }
        List<ApiSpace> spaces = parser.read(new StringReader(config), new TypeReference<List<ApiSpace>>() {
        });
        return new SyncResponse<>(SyncStatus.SUCCESS, spaces);
    }

    /**
     * Parses a configuration string into a LiveSpace object.
     *
     * @param config The configuration string to parse.
     * @return A LiveSpace object, or null if the configuration is empty.
     */
    protected SyncResponse<LaneSpace> parseSpace(String config) {
        if (config == null || config.isEmpty()) {
            return new SyncResponse<>(SyncStatus.NOT_FOUND, null);
        }
        LaneSpace space = parser.read(new StringReader(config), new TypeReference<LaneSpace>() {
        });
        return new SyncResponse<>(SyncStatus.SUCCESS, space);
    }

    /**
     * Generates a key based on the provided space ID.
     *
     * @param spaceId The ID of the space.
     * @return The generated key.
     */
    protected String getKey(String spaceId) {
        Map<String, Object> context = new HashMap<>();
        context.put("id", spaceId);
        return template.evaluate(context);
    }

}
