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
                .maxConcurrentCalls(policy.getMaxConcurrency())
                .maxWaitDuration(Duration.ofMillis(policy.getMaxWaitMs()))
                .build();
        BulkheadRegistry registry = BulkheadRegistry.of(config);
        bulkhead = registry.bulkhead(policy.getName());
    }

    @Override
    public boolean acquire() {
        return bulkhead.tryAcquirePermission();
    }
}
