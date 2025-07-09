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
import com.jd.live.agent.core.config.AgentPath;
import com.jd.live.agent.core.exception.SyncException;
import com.jd.live.agent.core.instance.Location;
import com.jd.live.agent.core.parser.TypeReference;
import com.jd.live.agent.governance.policy.live.db.LiveDatabaseSpec;
import com.jd.live.agent.governance.subscription.policy.PolicyEvent;
import com.jd.live.agent.governance.subscription.policy.PolicyEvent.EventType;
import com.jd.live.agent.governance.subscription.policy.PolicyWatcher;

import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;

import static com.jd.live.agent.governance.service.sync.SyncKey.LiveSpaceKey;

/**
 * An abstract class that provides a base implementation for synchronizing LiveDatabaseSpec objects using subscriptions.
 */
public abstract class AbstractLiveDatabaseSyncer<K1 extends LiveSpaceKey> extends AbstractSyncer<K1, LiveDatabaseSpec> {

    private static final Logger logger = LoggerFactory.getLogger(AbstractLiveDatabaseSyncer.class);

    @Override
    public String getType() {
        return PolicyWatcher.TYPE_LIVE_DATABASE_POLICY;
    }

    @Override
    protected void startSync() throws Exception {
        Location location = application.getLocation();
        String liveSpaceId = location == null ? null : location.getLiveSpaceId();
        if (liveSpaceId != null && !liveSpaceId.isEmpty()) {
            syncDatabase(liveSpaceId);
        }
    }

    /**
     * Get the filename for space id.
     */
    protected String getFileName(String spaceId) {
        return "live-database-" + spaceId + ".json";
    }

    /**
     * Creates a key for synchronizing a single LiveDatabaseSpec object.
     *
     * @param spaceId The ID of the LiveDatabaseSpec object to synchronize.
     * @return A unique identifier for the specified LiveDatabaseSpec object.
     */
    protected abstract K1 createSpaceKey(String spaceId);

    /**
     * Synchronizes a single LiveDatabaseSpec object with the remote server.
     *
     * @param spaceId The ID of the LiveDatabaseSpec object to synchronize.
     */
    protected void syncDatabase(String spaceId) {
        Subscription<K1, LiveDatabaseSpec> subscription = new Subscription<>(getName(), createSpaceKey(spaceId), application.getName(), r -> onDatabaseResponse(r, spaceId));
        if (subscriptions.put(spaceId, subscription) == null) {
            syncer.sync(subscription);
        }
    }

    /**
     * Handles the response from the sync operation for a LiveDatabaseSpec object.
     *
     * @param response The SyncResponse object containing the response from the sync operation.
     * @param spaceId  The space id.
     */
    protected void onDatabaseResponse(SyncResponse<LiveDatabaseSpec> response, String spaceId) {
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
     * Updates the specified LiveDatabaseSpec object.
     *
     * @param spaceId The space id.
     * @param spec    The LiveDatabaseSpec object to update.
     */
    protected void updateSpace(String spaceId, LiveDatabaseSpec spec) {
        if (spec == null) {
            deleteSpace(spaceId);
        } else {
            Subscription<K1, LiveDatabaseSpec> subscription = subscriptions.get(spec.getId());
            long version = spec.getVersion();
            if (subscription != null) {
                logger.info("Success syncing live database policy, spaceId={}", spaceId);
                synchronized (subscription) {
                    if (subscription.getVersion() < version) {
                        subscription.setVersion(version);
                        publish(PolicyEvent.builder()
                                .type(EventType.UPDATE_ITEM)
                                .name(spec.getId())
                                .value(spec)
                                .description("live database " + spec.getId())
                                .watcher(getName())
                                .build());
                    }
                }
            }
        }
    }

    /**
     * Deletes a live space by its ID.
     *
     * @param spaceId The ID of the live space to delete.
     */
    protected void deleteSpace(String spaceId) {
        Subscription<K1, LiveDatabaseSpec> subscription = subscriptions.remove(spaceId);
        if (subscription != null) {
            logger.info("live database policy is removed, spaceId={}", spaceId);
            synchronized (subscription) {
                syncer.remove(subscription);
                publish(PolicyEvent.builder()
                        .type(EventType.DELETE_ITEM)
                        .name(spaceId)
                        .description("live database " + spaceId)
                        .watcher(getName())
                        .build());
            }
        }
    }

    /**
     * Parses a configuration string into a LiveDatabaseSpec object.
     *
     * @param key    the key associated with this configuration.
     * @param config the configuration string to parse.
     * @return A LiveDatabaseSpec object, or null if the configuration is empty.
     */
    protected SyncResponse<LiveDatabaseSpec> parseDatabase(K1 key, String config) {
        if (config == null || config.isEmpty()) {
            return new SyncResponse<>(SyncStatus.NOT_FOUND, null);
        }
        saveConfig(config, AgentPath.DIR_POLICY_LIVE_DATABASE, getFileName(key.getId()));
        LiveDatabaseSpec space = parser.read(new StringReader(config), new TypeReference<LiveDatabaseSpec>() {
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
        context.put(APPLICATION, application.getName());
        return template.render(context);
    }

}
