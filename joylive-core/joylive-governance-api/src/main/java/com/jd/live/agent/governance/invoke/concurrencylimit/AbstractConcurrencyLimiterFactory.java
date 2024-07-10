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
package com.jd.live.agent.governance.invoke.concurrencylimit;

import com.jd.live.agent.core.inject.annotation.Inject;
import com.jd.live.agent.core.util.time.Timer;
import com.jd.live.agent.governance.config.GovernanceConfig;
import com.jd.live.agent.governance.policy.service.limit.ConcurrencyLimitPolicy;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * AbstractConcurrencyLimiterFactory
 */
public abstract class AbstractConcurrencyLimiterFactory implements ConcurrencyLimiterFactory {

    @Inject(Timer.COMPONENT_TIMER)
    private Timer timer;

    @Inject(GovernanceConfig.COMPONENT_GOVERNANCE_CONFIG)
    private GovernanceConfig governanceConfig;

    private final AtomicBoolean recycled = new AtomicBoolean(false);

    private final Map<Long, AtomicReference<ConcurrencyLimiter>> limiters = new ConcurrentHashMap<>();

    @Override
    public ConcurrencyLimiter get(ConcurrencyLimitPolicy policy) {
        if (policy == null) {
            return null;
        }
        AtomicReference<ConcurrencyLimiter> reference = limiters.computeIfAbsent(policy.getId(), n -> new AtomicReference<>());
        ConcurrencyLimiter concurrencyLimiter = reference.get();
        if (concurrencyLimiter != null && concurrencyLimiter.getPolicy().getVersion() == policy.getVersion()) {
            return concurrencyLimiter;
        }
        ConcurrencyLimiter newLimiter = create(policy);
        while (true) {
            concurrencyLimiter = reference.get();
            if (concurrencyLimiter == null || concurrencyLimiter.getPolicy().getVersion() < policy.getVersion()) {
                if (reference.compareAndSet(concurrencyLimiter, newLimiter)) {
                    concurrencyLimiter = newLimiter;
                    if (recycled.compareAndSet(false, true)) {
                        addRecycler();
                    }
                    break;
                }
            } else {
                break;
            }
        }
        return concurrencyLimiter;
    }

    /**
     * Schedules a recurring task to recycle concurrency limiters based on their expiration time.
     * This method retrieves the clean interval from the configuration and sets up a delayed task
     * that calls the {@link #recycle()} method and reschedules itself.
     */
    private void addRecycler() {
        long cleanInterval = governanceConfig.getServiceConfig().getConcurrencyLimiter().getCleanInterval();
        timer.delay("recycle-concurrency-limiter", cleanInterval, () -> {
            recycle();
            addRecycler();
        });
    }

    /**
     * Recycles expired concurrency limiters. This method checks each concurrency limiter to see if it has
     * expired based on the current time and the configured expiration time. If a concurrency limiter
     * has exceeded its expiration time, it is removed from the collection.
     */
    private void recycle() {
        long expireTime = governanceConfig.getServiceConfig().getConcurrencyLimiter().getExpireTime();
        for (Map.Entry<Long, AtomicReference<ConcurrencyLimiter>> entry : limiters.entrySet()) {
            AtomicReference<ConcurrencyLimiter> reference = entry.getValue();
            ConcurrencyLimiter limiter = reference.get();
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
}
