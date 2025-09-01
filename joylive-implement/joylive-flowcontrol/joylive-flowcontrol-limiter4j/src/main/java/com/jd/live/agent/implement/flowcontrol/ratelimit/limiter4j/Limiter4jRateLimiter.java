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
package com.jd.live.agent.implement.flowcontrol.ratelimit.limiter4j;

import com.jd.live.agent.bootstrap.logger.Logger;
import com.jd.live.agent.bootstrap.logger.LoggerFactory;
import com.jd.live.agent.core.util.option.MapOption;
import com.jd.live.agent.core.util.option.Options;
import com.jd.live.agent.governance.config.RateLimiterConfig;
import com.jd.live.agent.governance.invoke.ratelimit.AbstractRateLimiter;
import com.jd.live.agent.governance.policy.service.limit.RateLimitPolicy;
import com.jd.live.agent.governance.policy.service.limit.SlidingWindow;
import com.jd.live.agent.implement.flowcontrol.ratelimit.limiter4j.client.GrpcConfig;
import com.jd.live.agent.implement.flowcontrol.ratelimit.limiter4j.client.GrpcTokenClient;
import com.jd.live.agent.implement.flowcontrol.ratelimit.limiter4j.client.GrpcTokenClientManager;

import java.util.concurrent.TimeUnit;

/**
 * Rate limiter implementation based on gRPC token service
 *
 * @since 1.9.0
 */
public class Limiter4jRateLimiter extends AbstractRateLimiter {

    private static final Logger logger = LoggerFactory.getLogger(Limiter4jRateLimiter.class);

    private final GrpcTokenClientManager clientManager;
    private final GrpcTokenClient client;
    private final RateLimitPolicy policy;
    private final SlidingWindow window;
    private final String bucketId;

    public Limiter4jRateLimiter(GrpcTokenClientManager clientManager, RateLimitPolicy policy, RateLimiterConfig config, SlidingWindow window) {
        super(policy, TimeUnit.MILLISECONDS);
        this.clientManager = clientManager;
        this.policy = policy;
        this.window = window;
        // Create gRPC configuration
        this.client = clientManager.getOrCreateClient(new GrpcConfig(policy.getId(), new Options(this.option, new MapOption(config.getConfigs()))));
        // Create token bucket in constructor
        this.bucketId = client.createTokenBucket(policy, window);
    }

    @Override
    protected boolean doAcquire(int permits, long timeout, TimeUnit timeUnit) {
        if (!client.isConnected()) {
            // Allow by default when client is unavailable
            return true;
        }
        // Update last access time
        client.setLastAccessTime(System.currentTimeMillis());
        try {
            // Call gRPC service to acquire tokens
            return client.acquireTokens(bucketId, permits, timeUnit.toMillis(timeout));
        } catch (Exception e) {
            // Allow by default in case of exception
            logger.error("Error acquiring token for policy: {}, permits: {}", policy.getId(), permits, e);
            return true;
        }
    }

    @Override
    protected void doClose() {
        // Delete token bucket
        client.deleteTokenBucket(bucketId);
    }
}