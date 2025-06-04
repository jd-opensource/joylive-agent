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
package com.jd.live.agent.governance.invoke.filter;

import com.jd.live.agent.core.extension.ExtensionInitializer;
import com.jd.live.agent.core.inject.annotation.Inject;
import com.jd.live.agent.governance.config.GovernanceConfig;
import com.jd.live.agent.governance.invoke.Invocation;
import com.jd.live.agent.governance.invoke.ratelimit.RateLimiter;
import com.jd.live.agent.governance.invoke.ratelimit.RateLimiterFactory;
import com.jd.live.agent.governance.policy.service.ServicePolicy;
import com.jd.live.agent.governance.policy.service.limit.RateLimitPolicy;
import com.jd.live.agent.governance.request.ServiceRequest;
import com.jd.live.agent.governance.invoke.auth.Permission;

import java.util.List;
import java.util.Map;

/**
 * RateLimitFilter
 */
public class AbstractRateLimitFilter implements ExtensionInitializer {

    @Inject
    protected Map<String, RateLimiterFactory> factories;

    @Inject(nullable = true)
    protected RateLimiterFactory defaultFactory;

    @Inject(GovernanceConfig.COMPONENT_GOVERNANCE_CONFIG)
    protected GovernanceConfig governanceConfig;

    private String defaultType;

    @Override
    public void initialize() {
        defaultType = governanceConfig.getServiceConfig().getRateLimiter().getType();
    }

    /**
     * Attempts to acquire rate limit permits for the service invocation.
     * Checks all applicable rate limit policies and rejects the request if any limit is exceeded.
     *
     * @param <R>        the type of service request
     * @param invocation the service invocation context
     * @return AcquireResult indicating success or failure with rejection reason
     */
    protected <R extends ServiceRequest> Permission acquire(Invocation<R> invocation) {
        ServicePolicy servicePolicy = invocation.getServiceMetadata().getServicePolicy();
        List<RateLimitPolicy> policies = servicePolicy == null ? null : servicePolicy.getRateLimitPolicies();
        if (null != policies && !policies.isEmpty()) {
            for (RateLimitPolicy policy : policies) {
                // match logic
                if (policy.match(invocation)) {
                    RateLimiter rateLimiter = getRateLimiter(policy);
                    if (null != rateLimiter && !rateLimiter.acquire()) {
                        return Permission.failure("The request is rejected by " + rateLimiter.getClass().getSimpleName());
                    }
                }
            }
        }
        return Permission.success();
    }

    /**
     * Retrieves a rate limiter based on the given policy.
     * If the policy's realize type is not specified, it falls back to the default type
     * from the service configuration. If the factory for the specified type is not found,
     * it uses the first available factory.
     *
     * @param policy the rate limit policy.
     * @return the rate limiter instance based on the given policy.
     */
    private RateLimiter getRateLimiter(RateLimitPolicy policy) {
        String type = policy.getRealizeType() == null || policy.getRealizeType().isEmpty() ? defaultType : policy.getRealizeType();
        RateLimiterFactory factory = type != null ? factories.get(type) : null;
        factory = factory == null ? defaultFactory : factory;
        return factory == null ? null : factory.get(policy);
    }

}
