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
package com.jd.live.agent.implement.flowcontrol.ratelimit.limiter4j;

import com.jd.live.agent.core.extension.annotation.Extension;
import com.jd.live.agent.core.inject.annotation.Inject;
import com.jd.live.agent.core.util.cache.LazyObject;
import com.jd.live.agent.core.util.time.Timer;
import com.jd.live.agent.governance.config.ServiceConfig;
import com.jd.live.agent.governance.invoke.ratelimit.AbstractRateLimiterFactory;
import com.jd.live.agent.governance.invoke.ratelimit.RateLimiter;
import com.jd.live.agent.governance.policy.service.limit.RateLimitPolicy;
import com.jd.live.agent.governance.policy.service.limit.SlidingWindow;
import com.jd.live.agent.implement.flowcontrol.ratelimit.limiter4j.client.GrpcTokenClientManager;

import java.util.List;

/**
 * Limiter4j rate limiter factory.
 *
 * @since 1.9.0
 */
@Extension(value = "Limiter4j")
public class Limiter4jRateLimiterFactory extends AbstractRateLimiterFactory {

    @Inject(Timer.COMPONENT_TIMER)
    private Timer timer;

    @Inject(ServiceConfig.COMPONENT_SERVICE_CONFIG)
    private ServiceConfig config;

    private final LazyObject<GrpcTokenClientManager> cache = LazyObject.of(() -> new GrpcTokenClientManager(timer, config.getRateLimiter()));

    @Override
    protected RateLimiter create(RateLimitPolicy policy) {
        List<SlidingWindow> windows = policy.getSlidingWindows();
        GrpcTokenClientManager manager = cache.get();
        return windows.size() == 1
                ? new Limiter4jRateLimiter(manager, policy, windows.get(0))
                : new Limiter4jRateLimiterGroup(manager, policy);
    }

}