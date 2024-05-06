package com.jd.live.agent.governance.invoke.retry;

import com.jd.live.agent.core.extension.annotation.Extensible;
import com.jd.live.agent.governance.policy.service.failover.FailoverPolicy;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

/**
 * RetrierFactory
 *
 * @since 1.0.0
 */
@Extensible("RetrierFactory")
public interface RetrierFactory {

    Map<Long, AtomicReference<Retrier>> RETRIERS = new ConcurrentHashMap<>();

    default Retrier get(FailoverPolicy policy) {
        if (policy == null) {
            return null;
        }
        AtomicReference<Retrier> reference = RETRIERS.computeIfAbsent(policy.getId(), n -> new AtomicReference<>());
        Retrier retrier = reference.get();
        if (retrier != null && retrier.getPolicy().getVersion() >= policy.getVersion()) {
            return retrier;
        }
        Retrier newLimiter = create(policy);
        while (true) {
            retrier = reference.get();
            if (retrier == null || retrier.getPolicy().getVersion() < policy.getVersion()) {
                if (reference.compareAndSet(retrier, newLimiter)) {
                    retrier = newLimiter;
                    break;
                }
            }
        }
        return retrier;
    }

    /**
     * Create Retrier
     *
     * @param failoverPolicy Failure retry policy
     * @return Retrier
     */
    Retrier create(FailoverPolicy failoverPolicy);

}
