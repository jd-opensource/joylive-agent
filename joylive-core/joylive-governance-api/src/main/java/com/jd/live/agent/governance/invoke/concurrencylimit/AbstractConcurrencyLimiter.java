package com.jd.live.agent.governance.invoke.concurrencylimit;

import com.jd.live.agent.governance.policy.service.limit.ConcurrencyLimitPolicy;
import lombok.Getter;

/**
 * AbstractConcurrencyLimiter
 *
 * @since 1.0.0
 */
@Getter
public abstract class AbstractConcurrencyLimiter implements ConcurrencyLimiter {

    private final ConcurrencyLimitPolicy policy;

    private long lastAcquireTime;

    public AbstractConcurrencyLimiter(ConcurrencyLimitPolicy policy) {
        this.policy = policy;
    }

    @Override
    public boolean acquire() {
        lastAcquireTime = System.currentTimeMillis();
        return doAcquire();
    }

    /**
     * Performs the actual acquisition logic.
     * Subclasses must implement this method to define the specific acquisition behavior.
     *
     * @return true if the acquisition is successful, false otherwise.
     */
    protected abstract boolean doAcquire();
}
