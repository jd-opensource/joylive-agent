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
package com.jd.live.agent.governance.invoke.ratelimit;

import com.jd.live.agent.core.inject.annotation.Inject;
import com.jd.live.agent.core.util.time.Timer;
import com.jd.live.agent.governance.config.GovernanceConfig;
import com.jd.live.agent.governance.policy.service.limit.RateLimitPolicy;
import com.jd.live.agent.governance.policy.service.limit.SlidingWindow;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * AbstractLimiterFactory provides a base implementation for factories that create and manage rate limiters.
 * It uses a thread-safe map to store and retrieve rate limiters associated with specific rate limit policies.
 * This class is designed to be extended by concrete factory implementations that provide the actual
 * rate limiter creation logic.
 *
 * @since 1.0.0
 */
public abstract class AbstractRateLimiterFactory implements RateLimiterFactory {

    private final Map<Long, AtomicReference<RateLimiter>> limiters = new ConcurrentHashMap<>();

    @Inject(Timer.COMPONENT_TIMER)
    private Timer timer;

    @Inject(GovernanceConfig.COMPONENT_GOVERNANCE_CONFIG)
    private GovernanceConfig governanceConfig;

    private final AtomicBoolean recycled = new AtomicBoolean(false);

    @Override
    public RateLimiter get(RateLimitPolicy policy) {
        if (policy == null) {
            return null;
        }
        List<SlidingWindow> windows = policy.getSlidingWindows();
        if (windows == null || windows.isEmpty()) {
            return null;
        }
        AtomicReference<RateLimiter> reference = limiters.computeIfAbsent(policy.getId(), n -> new AtomicReference<>());
        RateLimiter rateLimiter = reference.get();
        if (rateLimiter != null && rateLimiter.getPolicy().getVersion() == policy.getVersion()) {
            return rateLimiter;
        }
        RateLimiter newLimiter = create(policy);
        while (true) {
            rateLimiter = reference.get();
            if (rateLimiter == null || rateLimiter.getPolicy().getVersion() < policy.getVersion()) {
                if (reference.compareAndSet(rateLimiter, newLimiter)) {
                    rateLimiter = newLimiter;
                    if (recycled.compareAndSet(false, true)) {
                        addRecycler();
                    }
                    break;
                }
            } else {
                break;
            }
        }
        return rateLimiter;
    }

    /**
     * Schedules a recurring task to recycle rate limiters based on their expiration time.
     * This method retrieves the clean interval from the configuration and sets up a delayed task
     * that calls the {@link #recycle()} method and reschedules itself.
     */
    private void addRecycler() {
        long cleanInterval = governanceConfig.getServiceConfig().getRateLimiter().getCleanInterval();
        timer.delay("recycle-rate-limiter", cleanInterval, () -> {
            recycle();
            addRecycler();
        });
    }

    /**
     * Recycles expired rate limiters. This method checks each concurrency limiter to see if it has
     * expired based on the current time and the configured expiration time. If a rate limiter
     * has exceeded its expiration time, it is removed from the collection.
     */
    private void recycle() {
        long expireTime = governanceConfig.getServiceConfig().getRateLimiter().getExpireTime();
        for (Map.Entry<Long, AtomicReference<RateLimiter>> entry : limiters.entrySet()) {
            AtomicReference<RateLimiter> reference = entry.getValue();
            RateLimiter limiter = reference.get();
            if (limiter != null && (System.currentTimeMillis() - limiter.getLastAcquireTime()) > expireTime) {
                reference = limiters.remove(entry.getKey());
                if (reference != null) {
                    limiter = reference.get();
                    if (limiter != null && (System.currentTimeMillis() - limiter.getLastAcquireTime()) <= expireTime) {
                        limiters.putIfAbsent(entry.getKey(), reference);
                    }
                }
            }
        }
    }

    /**
     * Creates a new rate limiter instance based on the provided rate limit policy.
     * This method is abstract and must be implemented by subclasses to provide the specific
     * rate limiter creation logic.
     *
     * @param policy The rate limit policy to be used for creating the rate limiter.
     * @return A new rate limiter instance that enforces the given policy.
     */
    protected abstract RateLimiter create(RateLimitPolicy policy);

}

