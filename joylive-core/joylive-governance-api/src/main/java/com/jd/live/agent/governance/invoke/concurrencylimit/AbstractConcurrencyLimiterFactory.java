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
import com.jd.live.agent.governance.policy.PolicySupplier;
import com.jd.live.agent.governance.policy.service.ServicePolicy;
import com.jd.live.agent.governance.policy.service.limit.ConcurrencyLimitPolicy;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicReference;

/**
 * AbstractConcurrencyLimiterFactory
 */
public abstract class AbstractConcurrencyLimiterFactory implements ConcurrencyLimiterFactory {

    @Inject(Timer.COMPONENT_TIMER)
    private Timer timer;

    @Inject(PolicySupplier.COMPONENT_POLICY_SUPPLIER)
    private PolicySupplier policySupplier;

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
            if (concurrencyLimiter == null || concurrencyLimiter.getPolicy().getVersion() != policy.getVersion()) {
                if (reference.compareAndSet(concurrencyLimiter, newLimiter)) {
                    concurrencyLimiter = newLimiter;
                    addRecycleTask(policy);
                    break;
                }
            }
        }
        return concurrencyLimiter;
    }

    private void addRecycleTask(ConcurrencyLimitPolicy policy) {
        long delay = 60000 + ThreadLocalRandom.current().nextInt(60000 * 4);
        timer.delay("recycle-ratelimiter-" + policy.getId(), delay, () -> recycle(policy));
    }

    private void recycle(ConcurrencyLimitPolicy policy) {
        AtomicReference<ConcurrencyLimiter> ref = limiters.get(policy.getId());
        ConcurrencyLimiter limiter = ref == null ? null : ref.get();
        if (limiter != null && policySupplier != null) {
            ServicePolicy servicePolicy = policySupplier.getPolicy().getServicePolicy(policy.getUri());
            boolean exists = false;
            if (servicePolicy != null && servicePolicy.getRateLimitPolicies() != null) {
                for (ConcurrencyLimitPolicy limitPolicy : servicePolicy.getConcurrencyLimitPolicies()) {
                    if (Objects.equals(limitPolicy.getId(), policy.getId())) {
                        exists = true;
                        break;
                    }
                }
            }
            if (!exists) {
                limiters.remove(policy.getId());
            } else {
                addRecycleTask(policy);
            }
        }
    }
}
