package com.jd.live.agent.governance.invoke.concurrencylimit;

import com.jd.live.agent.governance.invoke.permission.AbstractLicensee;
import com.jd.live.agent.governance.policy.service.limit.ConcurrencyLimitPolicy;

/**
 * AbstractConcurrencyLimiter
 *
 * @since 1.0.0
 */
public abstract class AbstractConcurrencyLimiter extends AbstractLicensee<ConcurrencyLimitPolicy> implements ConcurrencyLimiter {

    public AbstractConcurrencyLimiter(ConcurrencyLimitPolicy policy) {
        this.policy = policy;
    }

    @Override
    public boolean acquire() {
        if (!started.get()) {
            return true;
        }
        lastAccessTime = System.currentTimeMillis();
        return doAcquire();
    }

    /**
     * Performs the actual acquisition logic.
     * Subclasses must implement this method to define the specific acquisition behavior.
     *
     * @return true if the acquisition is successful, false otherwise.
     */
    protected abstract boolean doAcquire();

    @Override
    public void complete() {
    }

}
