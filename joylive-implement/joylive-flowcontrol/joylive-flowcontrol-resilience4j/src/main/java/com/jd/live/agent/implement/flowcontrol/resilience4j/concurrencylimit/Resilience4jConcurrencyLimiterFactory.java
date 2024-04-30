package com.jd.live.agent.implement.flowcontrol.resilience4j.concurrencylimit;

import com.jd.live.agent.core.extension.annotation.Extension;
import com.jd.live.agent.core.inject.annotation.Injectable;
import com.jd.live.agent.governance.invoke.concurrencylimit.ConcurrencyLimiter;
import com.jd.live.agent.governance.invoke.concurrencylimit.ConcurrencyLimiterFactory;
import com.jd.live.agent.governance.policy.service.limit.ConcurrencyLimitPolicy;

/**
 * Resilience4jConcurrencyLimiterFactory
 *
 * @since 1.0.0
 */
@Injectable
@Extension(value = "Resilience4j")
public class Resilience4jConcurrencyLimiterFactory implements ConcurrencyLimiterFactory {

    /**
     * {@inheritDoc}
     */
    @Override
    public ConcurrencyLimiter create(ConcurrencyLimitPolicy policy) {
        return new Resilience4jConcurrencyLimiter(policy);
    }
}
