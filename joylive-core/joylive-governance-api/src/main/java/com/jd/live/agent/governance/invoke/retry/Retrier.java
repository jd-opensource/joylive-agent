package com.jd.live.agent.governance.invoke.retry;

import com.jd.live.agent.governance.policy.service.failover.FailoverPolicy;

import java.util.function.Supplier;

/**
 * Retrier
 *
 * @since 1.0.0
 */
public interface Retrier {

    /**
     * Execute retry logic
     *
     * @param supplier Retry logic
     * @param <T>      Response type
     * @return Response
     */
    <T> T execute(Supplier<T> supplier);

    /**
     * Get failover policy
     *
     * @return policy
     */
    FailoverPolicy getPolicy();
}
