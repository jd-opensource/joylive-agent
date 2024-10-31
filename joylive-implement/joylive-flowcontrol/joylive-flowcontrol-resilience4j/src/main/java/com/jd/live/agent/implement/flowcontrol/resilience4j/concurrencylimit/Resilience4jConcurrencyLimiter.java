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
package com.jd.live.agent.implement.flowcontrol.resilience4j.concurrencylimit;

import com.jd.live.agent.governance.invoke.concurrencylimit.AbstractConcurrencyLimiter;
import com.jd.live.agent.governance.policy.service.limit.ConcurrencyLimitPolicy;
import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadConfig;
import io.github.resilience4j.bulkhead.BulkheadRegistry;

import java.time.Duration;

/**
 * Resilience4jConcurrencyLimiter
 *
 * @author Zhiguo.Chen
 * @since 1.0.0
 */
public class Resilience4jConcurrencyLimiter extends AbstractConcurrencyLimiter {

    private final Bulkhead bulkhead;

    public Resilience4jConcurrencyLimiter(ConcurrencyLimitPolicy policy) {
        super(policy);
        BulkheadConfig config = BulkheadConfig.custom()
                .maxConcurrentCalls(policy.getMaxConcurrency() == null ? 0 : policy.getMaxConcurrency())
                .maxWaitDuration(Duration.ofMillis(policy.getMaxWaitMs() == null || policy.getMaxWaitMs() < 0 ? 0 : policy.getMaxWaitMs()))
                .build();
        BulkheadRegistry registry = BulkheadRegistry.of(config);
        bulkhead = registry.bulkhead(policy.getName());
    }

    @Override
    public boolean doAcquire() {
        return bulkhead.tryAcquirePermission();
    }
}
