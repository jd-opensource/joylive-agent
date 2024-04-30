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

import com.jd.live.agent.core.extension.annotation.Extensible;
import com.jd.live.agent.governance.policy.service.limit.ConcurrencyLimitPolicy;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

/**
 * ConcurrencyLimiterFactory
 *
 * @since 1.0.0
 */
@Extensible("ConcurrencyLimiterFactory")
public interface ConcurrencyLimiterFactory {

    /**
     * A thread-safe map to store concurrency limiters associated with their respective policies.
     * The keys are the policy IDs, and the values are atomic references to the concurrency limiters.
     */
    Map<Long, AtomicReference<ConcurrencyLimiter>> CONCURRENCY_LIMITERS = new ConcurrentHashMap<>();

    /**
     * Retrieves a concurrency limiter for the given concurrency limit policy. If a concurrency limiter for the policy
     * already exists and its version is greater than or equal to the policy version, it is returned.
     * Otherwise, a new concurrency limiter is created using the {@link #create(ConcurrencyLimitPolicy)} method.
     *
     * @param policy The concurrency limit policy for which to retrieve or create a concurrency limiter.
     * @return A concurrency limiter that corresponds to the given policy, or null if the policy is null.
     */
    default ConcurrencyLimiter get(ConcurrencyLimitPolicy policy) {
        if (policy == null) {
            return null;
        }
        AtomicReference<ConcurrencyLimiter> reference = CONCURRENCY_LIMITERS.computeIfAbsent(policy.getId(), n -> new AtomicReference<>());
        ConcurrencyLimiter concurrencyLimiter = reference.get();
        if (concurrencyLimiter != null && concurrencyLimiter.getPolicy().getVersion() >= policy.getVersion()) {
            return concurrencyLimiter;
        }
        ConcurrencyLimiter newLimiter = create(policy);
        while (true) {
            concurrencyLimiter = reference.get();
            if (concurrencyLimiter == null || concurrencyLimiter.getPolicy().getVersion() < policy.getVersion()) {
                if (reference.compareAndSet(concurrencyLimiter, newLimiter)) {
                    concurrencyLimiter = newLimiter;
                    break;
                }
            }
        }
        return concurrencyLimiter;
    }

    /**
     * Retrieves a new instance of a {@link ConcurrencyLimiter} based on the provided
     * concurrency limit policy.
     *
     * @param policy the policy that defines the concurrency limiting rules.
     * @return a new instance of a concurrency limiter configured according to the policy.
     */
    ConcurrencyLimiter create(ConcurrencyLimitPolicy policy);

}
