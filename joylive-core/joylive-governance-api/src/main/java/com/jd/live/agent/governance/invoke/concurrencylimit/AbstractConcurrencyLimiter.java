package com.jd.live.agent.governance.invoke.concurrencylimit;

import com.jd.live.agent.governance.policy.service.limit.ConcurrencyLimitPolicy;

/**
 * AbstractConcurrencyLimiter
 *
 * @since 1.0.0
 */
public abstract class AbstractConcurrencyLimiter implements ConcurrencyLimiter {

    private final ConcurrencyLimitPolicy policy;

    public AbstractConcurrencyLimiter(ConcurrencyLimitPolicy policy) {
        this.policy = policy;
    }

    @Override
    public ConcurrencyLimitPolicy getPolicy() {
        return policy;
    }
}
