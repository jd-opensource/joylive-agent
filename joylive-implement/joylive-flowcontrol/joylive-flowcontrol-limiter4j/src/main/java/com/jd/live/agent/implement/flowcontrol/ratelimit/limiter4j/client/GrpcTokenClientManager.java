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

import java.util.concurrent.ConcurrentHashMap;

/**
 * gRPC Token Client Manager
 *
 * @since 1.9.0
 */
public class GrpcTokenClientManager {

    private static final Logger logger = LoggerFactory.getLogger(GrpcTokenClientManager.class);

    private final ConcurrentHashMap<String, GrpcTokenClient> clients = new ConcurrentHashMap<>();
    private final RateLimiterConfig config;

    public GrpcTokenClientManager(Timer timer, RateLimiterConfig config) {
        this.config = config;
        timer.schedule("Recycle-limiter4j", config.getCleanInterval(), this::cleanup);
    }

    /**
     * Get or create client
     *
     * @param config gRPC configuration
     * @return gRPC token client
     */
    public GrpcTokenClient getOrCreateClient(GrpcConfig config) {
        return clients.computeIfAbsent(config.getAddress(), k -> new GrpcTokenClient(config));
    }

    /**
     * Clean up idle clients
     */
    private void cleanup() {
        long currentTime = System.currentTimeMillis();
        clients.entrySet().removeIf(entry -> {
            GrpcTokenClient client = entry.getValue();
            long idleTime = currentTime - client.getLastAccessTime();
            if (idleTime > config.getExpireTime()) {
                logger.info("Closing idle gRPC client for address: {}, idle time: {}ms", client.getConfig().getAddress(), idleTime);
                client.close();
                return true;
            }
            return false;
        });
    }

}