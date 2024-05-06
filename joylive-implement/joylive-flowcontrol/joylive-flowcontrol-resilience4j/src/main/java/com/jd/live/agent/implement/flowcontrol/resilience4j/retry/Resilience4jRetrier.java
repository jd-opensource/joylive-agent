package com.jd.live.agent.implement.flowcontrol.resilience4j.retry;

import com.jd.live.agent.governance.invoke.retry.Retrier;
import com.jd.live.agent.governance.policy.service.failover.FailoverPolicy;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;

import java.time.Duration;
import java.util.function.Supplier;

/**
 * Resilience4jRetrier
 *
 * @since 1.0.0
 */
public class Resilience4jRetrier implements Retrier {

    private final FailoverPolicy policy;

    private final Retry retry;

    public Resilience4jRetrier(FailoverPolicy policy) {
        this.policy = policy;
        RetryConfig config = RetryConfig.custom()
                .maxAttempts(policy.getRetry())
                .waitDuration(Duration.ofMillis(policy.getTimeoutInMilliseconds()))
                // TODO
                // .retryOnResult(response -> response.getStatus() == 500)
                .failAfterMaxAttempts(true)
                .build();
        RetryRegistry registry = RetryRegistry.of(config);
        retry = registry.retry(policy.getId().toString());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> T execute(Supplier<T> supplier) {
        return retry.executeSupplier(supplier);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FailoverPolicy getPolicy() {
        return policy;
    }
}
