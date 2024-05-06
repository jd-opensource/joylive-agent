package com.jd.live.agent.implement.flowcontrol.resilience4j.retry;

import com.jd.live.agent.governance.invoke.retry.Retrier;
import com.jd.live.agent.governance.invoke.retry.RetrierFactory;
import com.jd.live.agent.governance.policy.service.failover.FailoverPolicy;

/**
 * Resilience4jRetrierFactory
 *
 * @since 1.0.0
 */
public class Resilience4jRetrierFactory implements RetrierFactory {

    @Override
    public Retrier create(FailoverPolicy failoverPolicy) {
        return new Resilience4jRetrier(failoverPolicy);
    }
}
