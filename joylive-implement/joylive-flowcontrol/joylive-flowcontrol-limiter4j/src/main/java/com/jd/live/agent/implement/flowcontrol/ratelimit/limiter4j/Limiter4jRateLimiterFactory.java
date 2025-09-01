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
import com.jd.live.agent.core.instance.Application;
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

    @Inject(Application.COMPONENT_APPLICATION)
    private Application application;

    private transient volatile GrpcTokenClientManager manager;

    @Override
    protected RateLimiter create(RateLimitPolicy policy) {
        List<SlidingWindow> windows = policy.getSlidingWindows();
        GrpcTokenClientManager manager = getManager();
        return windows.size() == 1
                ? new Limiter4jRateLimiter(manager, policy, config.getRateLimiter(), windows.get(0))
                : new Limiter4jRateLimiterGroup(manager, policy, config.getRateLimiter());
    }

    /**
     * Retrieves the singleton instance of {@link GrpcTokenClientManager}.
     *
     * @return The singleton instance of {@link GrpcTokenClientManager}.
     */
    private GrpcTokenClientManager getManager() {
        if (manager == null) {
            synchronized (this) {
                if (manager == null) {
                    manager = new GrpcTokenClientManager(timer, config.getRateLimiter());
                }
            }
        }
        return manager;
    }
}