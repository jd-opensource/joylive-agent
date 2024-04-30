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
package com.jd.live.agent.implement.flowcontrol.resilience4j.ratelimit;

import com.jd.live.agent.core.extension.annotation.Extension;
import com.jd.live.agent.core.inject.annotation.Injectable;
import com.jd.live.agent.governance.invoke.ratelimit.AbstractLimiterFactory;
import com.jd.live.agent.governance.invoke.ratelimit.RateLimiter;
import com.jd.live.agent.governance.policy.service.limit.RateLimitPolicy;
import com.jd.live.agent.governance.policy.service.limit.SlidingWindow;

import java.util.List;

/**
 * Resilience4jRateLimiterFactory
 *
 * @author Zhiguo.Chen
 * @since 1.0.0
 */
@Injectable
@Extension(value = "Resilience4j")
public class Resilience4jRateLimiterFactory extends AbstractLimiterFactory {

    /**
     * {@inheritDoc}
     */
    @Override
    protected RateLimiter create(RateLimitPolicy policy) {
        List<SlidingWindow> windows = policy.getSlidingWindows();
        return windows.size() == 1 ? new Resilience4jRateLimiter(policy, windows.get(0)) : new Resilience4jRateLimiterGroup(policy);
    }

}
