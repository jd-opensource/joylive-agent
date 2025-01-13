/*
 * Copyright (C) 2012 The Guava Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.jd.live.agent.governance.invoke.ratelimit.tokenbucket;

import com.jd.live.agent.governance.policy.service.limit.RateLimitPolicy;
import com.jd.live.agent.governance.policy.service.limit.SlidingWindow;

/**
 * SmoothBurstyLimiter
 * <p>
 * Source code implementation borrows from Guava's com.google.common.util.concurrent.SmoothRateLimiter.SmoothBursty
 * </p>
 *
 * @since 1.0.0
 */
public class SmoothBurstyLimiter extends TokenBucketLimiter {

    private static final String KEY_MAX_BURST_SECONDS = "maxBurstSeconds";

    private static final long DEFAULT_MAX_BURST_SECONDS = 1L;

    public SmoothBurstyLimiter(RateLimitPolicy limitPolicy, SlidingWindow slidingWindow) {
        super(limitPolicy, slidingWindow);
    }

    @Override
    protected double getMaxStoredPermits() {
        return getPermits(option.getPositive(KEY_MAX_BURST_SECONDS, DEFAULT_MAX_BURST_SECONDS));
    }
}
