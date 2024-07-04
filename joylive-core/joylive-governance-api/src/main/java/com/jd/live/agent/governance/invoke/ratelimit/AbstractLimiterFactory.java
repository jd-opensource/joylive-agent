/*
 * Copyright © ${year} ${owner} (${email})
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jd.live.agent.governance.invoke.ratelimit;

import com.jd.live.agent.core.inject.annotation.Inject;
import com.jd.live.agent.core.util.time.Timer;
import com.jd.live.agent.governance.policy.PolicyId;
import com.jd.live.agent.governance.policy.service.*;
import com.jd.live.agent.governance.policy.service.limit.RateLimitPolicy;
import com.jd.live.agent.governance.policy.service.limit.SlidingWindow;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

/**
 * AbstractLimiterFactory provides a base implementation for factories that create and manage rate limiters.
 * It uses a thread-safe map to store and retrieve rate limiters associated with specific rate limit policies.
 * This class is designed to be extended by concrete factory implementations that provide the actual
 * rate limiter creation logic.
 *
 * @since 1.0.0
 */
public abstract class AbstractLimiterFactory implements RateLimiterFactory {

    private final Map<Long, AtomicReference<RateLimiter>> rateLimiters = new ConcurrentHashMap<>();

    @Inject(Timer.COMPONENT_TIMER)
    private Timer timer;

    @Override
    public RateLimiter get(RateLimitPolicy policy, Function<String, Service> serviceFunc) {
        if (policy == null) {
            return null;
        }
        List<SlidingWindow> windows = policy.getSlidingWindows();
        if (windows == null || windows.isEmpty()) {
            return null;
        }
        AtomicReference<RateLimiter> reference = rateLimiters.computeIfAbsent(policy.getId(), n -> new AtomicReference<>());
        RateLimiter rateLimiter = reference.get();
        if (rateLimiter != null && rateLimiter.getPolicy().getVersion() == policy.getVersion()) {
            return rateLimiter;
        }
        RateLimiter newLimiter = create(policy);
        while (true) {
            rateLimiter = reference.get();
            if (rateLimiter == null || rateLimiter.getPolicy().getVersion() != policy.getVersion()) {
                if (reference.compareAndSet(rateLimiter, newLimiter)) {
                    rateLimiter = newLimiter;
                    addRecycleTask(policy, serviceFunc);
                    break;
                }
            }
        }
        return rateLimiter;
    }

    private void addRecycleTask(RateLimitPolicy policy, Function<String, Service> serviceFunc) {
        long delay = 60000 + ThreadLocalRandom.current().nextInt(60000 * 4);
        timer.delay("recycle-ratelimiter-" + policy.getId(), delay, () -> recycle(policy, serviceFunc));
    }

    private void recycle(RateLimitPolicy policy, Function<String, Service> serviceFunc) {
        AtomicReference<RateLimiter> ref = rateLimiters.get(policy.getId());
        RateLimiter limiter = ref == null ? null : ref.get();
        if (limiter != null && serviceFunc != null) {
            String serviceName = policy.getTag(PolicyId.KEY_SERVICE_NAME);
            String serviceGroup = policy.getTag(PolicyId.KEY_SERVICE_GROUP);
            String servicePath = policy.getTag(PolicyId.KEY_SERVICE_PATH);
            String serviceMethod = policy.getTag(PolicyId.KEY_SERVICE_METHOD);

            Service service = serviceFunc.apply(serviceName);
            ServiceGroup group = service == null ? null : service.getGroup(serviceGroup);
            ServicePath path = group == null ? null : group.getPath(servicePath);
            ServiceMethod method = path == null ? null : path.getMethod(serviceMethod);

            ServicePolicy servicePolicy = method != null ? method.getServicePolicy() : null;
            servicePolicy = servicePolicy == null && path != null ? path.getServicePolicy() : servicePolicy;
            servicePolicy = servicePolicy == null && group != null ? group.getServicePolicy() : servicePolicy;

            boolean exists = false;
            if (servicePolicy != null && servicePolicy.getRateLimitPolicies() != null) {
                for (RateLimitPolicy rateLimitPolicy : servicePolicy.getRateLimitPolicies()) {
                    if (Objects.equals(rateLimitPolicy.getId(), policy.getId())) {
                        exists = true;
                        break;
                    }
                }
            }
            if (!exists) {
                rateLimiters.remove(policy.getId());
            } else {
                addRecycleTask(policy, serviceFunc);
            }
        }
    }

    /**
     * Creates a new rate limiter instance based on the provided rate limit policy.
     * This method is abstract and must be implemented by subclasses to provide the specific
     * rate limiter creation logic.
     *
     * @param policy The rate limit policy to be used for creating the rate limiter.
     * @return A new rate limiter instance that enforces the given policy.
     */
    protected abstract RateLimiter create(RateLimitPolicy policy);

}

