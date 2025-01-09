package com.jd.live.agent.governance.invoke.concurrencylimit;

import com.jd.live.agent.governance.invoke.permission.AbstractLicensee;
import com.jd.live.agent.governance.policy.service.limit.ConcurrencyLimitPolicy;
import lombok.Getter;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * AbstractConcurrencyLimiter
 *
 * @since 1.0.0
 */
public abstract class AbstractConcurrencyLimiter extends AbstractLicensee<ConcurrencyLimitPolicy> implements ConcurrencyLimiter {

    @Getter
    protected long lastAccessTime;

    protected final AtomicBoolean started = new AtomicBoolean(true);

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

    @Override
    public void close() {
        if (started.compareAndSet(true, false)) {
            doClose();
        }
    }

    /**
     * Performs the actual acquisition logic.
     * Subclasses must implement this method to define the specific acquisition behavior.
     *
     * @return true if the acquisition is successful, false otherwise.
     */
    protected abstract boolean doAcquire();

    /**
     * Closes the limiter.
     */
    protected void doClose() {

    }
}
