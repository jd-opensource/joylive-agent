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
package com.jd.live.agent.implement.flowcontrol.ratelimit.limiter4j.client;

import com.jd.live.agent.bootstrap.logger.Logger;
import com.jd.live.agent.bootstrap.logger.LoggerFactory;
import com.jd.live.agent.core.util.time.Timer;
import com.jd.live.agent.governance.config.RateLimiterConfig;
import lombok.Getter;

import java.util.concurrent.ConcurrentHashMap;

/**
 * gRPC Token Client Manager
 *
 * @since 1.9.0
 */
public class GrpcTokenClientManager {

    private static final Logger logger = LoggerFactory.getLogger(GrpcTokenClientManager.class);

    @Getter
    private final RateLimiterConfig config;
    private final Timer timer;
    private final ConcurrentHashMap<String, GrpcTokenClient> clients = new ConcurrentHashMap<>();

    public GrpcTokenClientManager(Timer timer, RateLimiterConfig config) {
        this.timer = timer;
        this.config = config;
    }

    /**
     * Get or create client
     *
     * @param config gRPC configuration
     * @return gRPC token client
     */
    public GrpcTokenClient getOrCreateClient(GrpcConfig config) {
        return clients.computeIfAbsent(config.getAddress(), k -> new GrpcTokenClient(config, this::removeClient));
    }

    /**
     * Removes a Redis client if its reference count is zero and it has expired.
     *
     * @param client the Redis client to be removed
     */
    private void removeClient(final GrpcTokenClient client) {
        clients.computeIfPresent(client.getAddress(), (c, v) -> {
            if (v == client && v.isUseless()) {
                addTask(v);
                return null;
            }
            return v;
        });
    }

    /**
     * Adds a task to the timer to recycle the grpc token client if its reference count is zero and it has expired.
     *
     * @param client the grpc token client to be recycled
     */
    private void addTask(GrpcTokenClient client) {
        timer.delay("Recycle-limiter4j-" + client.getAddress(), config.getClientCleanInterval(), () -> {
            if (client.isExpired(config.getClientExpireTime())) {
                client.shutdown();
            } else {
                addTask(client);
            }
        });
    }
}