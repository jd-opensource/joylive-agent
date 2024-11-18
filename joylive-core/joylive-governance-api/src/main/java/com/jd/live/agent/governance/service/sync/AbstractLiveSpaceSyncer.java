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
import com.jd.live.agent.governance.policy.live.LiveSpace;
import com.jd.live.agent.governance.service.sync.api.ApiSpace;

import java.io.StringReader;
import java.util.*;

import static com.jd.live.agent.governance.service.sync.SyncKey.LiveSpaceKey;

/**
 * An abstract class that provides a base implementation for synchronizing LiveSpace objects using subscriptions.
 */
public abstract class AbstractLiveSpaceSyncer<K1 extends LiveSpaceKey, K2 extends LiveSpaceKey> extends AbstractSyncer<K1, LiveSpace> {

    protected Syncer<K2, List<ApiSpace>> spaceListSyncer;

    protected Subscription<K2, List<ApiSpace>> spaceListSubscription;

    @Override
    public String getType() {
        return ConfigWatcher.TYPE_LIVE_SPACE;
    }

    @Override
    protected void startSync() throws Exception {
        spaceListSubscription = new Subscription<>(getName(), createSpaceListKey(), this::onSpaceListResponse);
        spaceListSyncer = createSpaceListSyncer();
        Location location = application.getLocation();
        String laneSpaceId = location == null ? null : location.getLiveSpaceId();
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
     * Creates a key for synchronizing a list of LiveSpace objects.
     *
     * @return A unique identifier for the list of LiveSpace objects.
     */
    protected abstract K2 createSpaceListKey();

    /**
     * Creates a key for synchronizing a single LiveSpace object.
     *
     * @param spaceId The ID of the LiveSpace object to synchronize.
     * @return A unique identifier for the specified LiveSpace object.
     */
    protected abstract K1 createSpaceKey(String spaceId);

    /**
     * Creates a Syncer object for synchronizing a list of ApiSpace objects.
     *
     * @return A Syncer object for synchronizing a list of ApiSpace objects.
     */
    protected abstract Syncer<K2, List<ApiSpace>> createSpaceListSyncer();

    /**
     * Synchronizes the list of LiveSpace objects with the remote server.
     */
    protected void syncSpaceList() {
        spaceListSyncer.sync(spaceListSubscription);
    }

    /**
     * Synchronizes a single LiveSpace object with the remote server.
     *
     * @param spaceId The ID of the LiveSpace object to synchronize.
     */
    protected void syncSpace(String spaceId) {
        Subscription<K1, LiveSpace> subscription = new Subscription<>(getName(), createSpaceKey(spaceId), r -> onSpaceResponse(r, spaceId));
        if (subscriptions.put(spaceId, subscription) == null) {
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
                throw new SyncException("Failed to sync live spaces policy, caused by the spaces is not found.");
            case ERROR:
                throw new SyncException(response.getError());
        }
    }

    /**
     * Handles the response from the sync operation for a LiveSpace object.
     *
     * @param response The SyncResponse object containing the response from the sync operation.
     * @param spaceId The space id.
     */
    protected void onSpaceResponse(SyncResponse<LiveSpace> response, String spaceId) {
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
     * Updates the list of LiveSpace objects based on the provided list of ApiSpace objects.
     *
     * @param apiSpaces The list of ApiSpace objects to update the LiveSpace objects from.
     */
    protected void updateSpaceList(List<ApiSpace> apiSpaces) {
        Set<String> spaces = new HashSet<>();
        for (ApiSpace apiSpace : apiSpaces) {
            spaces.add(apiSpace.getId());
            if (!subscriptions.containsKey(apiSpace.getId())) {
                syncSpace(apiSpace.getId());
            }
        }
        for (Map.Entry<String, Subscription<K1, LiveSpace>> entry : subscriptions.entrySet()) {
            if (!spaces.contains(entry.getKey())) {
                deleteSpace(entry.getKey());
            }
        }
    }

    /**
     * Updates the specified LiveSpace object.
     *
     * @param spaceId The space id.
     * @param space The LiveSpace object to update.
     */
    protected void updateSpace(String spaceId, LiveSpace space) {
        if (space == null) {
            deleteSpace(spaceId);
        } else {
            Subscription<K1, LiveSpace> subscription = subscriptions.get(space.getId());
            long version = space.getSpec().getVersion();
            if (subscription != null) {
                synchronized (subscription) {
                    if (subscription.getVersion() < version) {
                        subscription.setVersion(version);
                        publish(ConfigEvent.builder()
                                .type(EventType.UPDATE_ITEM)
                                .name(space.getId())
                                .value(space)
                                .description("live space " + space.getId())
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
        Subscription<K1, LiveSpace> subscription = subscriptions.remove(spaceId);
        if (subscription != null) {
            synchronized (subscription) {
                syncer.remove(subscription);
                publish(ConfigEvent.builder()
                        .type(EventType.DELETE_ITEM)
                        .name(spaceId)
                        .description("live space " + spaceId)
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
    protected SyncResponse<LiveSpace> parseSpace(String config) {
        if (config == null || config.isEmpty()) {
            return new SyncResponse<>(SyncStatus.NOT_FOUND, null);
        }
        LiveSpace space = parser.read(new StringReader(config), new TypeReference<LiveSpace>() {
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
