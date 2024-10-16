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
package com.jd.live.agent.governance.invoke.ratelimit.leakybucket;

import com.jd.live.agent.core.extension.annotation.Extension;
import com.jd.live.agent.core.inject.annotation.Injectable;
import com.jd.live.agent.governance.invoke.ratelimit.AbstractRateLimiterFactory;
import com.jd.live.agent.governance.invoke.ratelimit.RateLimiter;
import com.jd.live.agent.governance.policy.service.limit.RateLimitPolicy;
import com.jd.live.agent.governance.policy.service.limit.SlidingWindow;

import java.util.List;

/**
 * LeakyBucketLimiterFactory
 *
 * @since 1.4.0
 */
@Injectable
@Extension(value = "LeakyBucket")
public class LeakyBucketLimiterFactory extends AbstractRateLimiterFactory {

    @Override
    protected RateLimiter create(RateLimitPolicy policy) {
        List<SlidingWindow> windows = policy.getSlidingWindows();
        if (windows.size() == 1) {
            return new LeakyBucketLimiter(policy, windows.get(0));
        }
        return new LeakyBucketLimiterGroup(policy);
    }
}
