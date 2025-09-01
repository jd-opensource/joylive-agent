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
package com.jd.live.agent.implement.flowcontrol.ratelimit.redisson.client;

import com.jd.live.agent.core.util.time.Timer;
import com.jd.live.agent.governance.config.RateLimiterConfig;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manager class for Redis clients.
 * This class manages the creation, retrieval, and recycling of Redis clients based on their configurations.
 */
public class RedisClientManager {

    private final Timer timer;

    private final RateLimiterConfig config;

    private final Map<RedisConfig, RedisClient> clients = new ConcurrentHashMap<>();

    public RedisClientManager(Timer timer, RateLimiterConfig config) {
        this.timer = timer;
        this.config = config;
    }

    /**
     * Retrieves an existing Redis client for the given configuration or creates a new one if it does not exist.
     *
     * @param config the {@link RedisConfig} for the client
     * @return the Redis client for the given configuration
     */
    public RedisClient getOrCreateClient(RedisConfig config) {
        RedisClient client = clients.computeIfAbsent(config, c -> new RedisClient(c, this::removeClient));
        client.start();
        return client;
    }

    /**
     * Removes a Redis client if its reference count is zero and it has expired.
     *
     * @param client the Redis client to be removed
     */
    private void removeClient(final RedisClient client) {
        clients.computeIfPresent(client.getConfig(), (c, v) -> {
            if (v == client && v.isUseless()) {
                addTask(v);
                return null;
            }
            return v;
        });
    }

    /**
     * Adds a task to the timer to recycle the Redis client if its reference count is zero and it has expired.
     *
     * @param client the Redis client to be recycled
     */
    private void addTask(RedisClient client) {
        timer.delay("Recycle-redisson-" + client.getId(), config.getCleanInterval(), () -> {
            if (client.isExpired(config.getExpireTime())) {
                client.shutdown();
            } else {
                addTask(client);
            }
        });
    }

}
