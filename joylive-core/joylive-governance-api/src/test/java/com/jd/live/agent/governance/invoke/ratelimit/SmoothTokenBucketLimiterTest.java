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

import com.jd.live.agent.governance.invoke.ratelimit.tokenbucket.SmoothBurstyLimiter;
import com.jd.live.agent.governance.policy.service.limit.RateLimitPolicy;
import com.jd.live.agent.governance.policy.service.limit.SlidingWindow;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.concurrent.TimeUnit;

import static org.mockito.Mockito.mock;

/**
 * SmoothTokenBucketLimiterTest
 *
 * @author Zhiguo.Chen
 * @since 1.0.0
 */
public class SmoothTokenBucketLimiterTest {

    private SmoothBurstyLimiter limiter;

    @BeforeEach
    void setUp() {
        RateLimitPolicy mockPolicy = mock(RateLimitPolicy.class);
        limiter = new SmoothBurstyLimiter(mockPolicy, new SlidingWindow(10, 1000L));
    }

    @ParameterizedTest
    @CsvSource({"1,true", "1000,true"})
    void testAcquireWithPermits(int permits, boolean expected) {
        boolean result = limiter.acquire(permits);
        assert result == expected;
    }

    @Test
    void testAcquireWithPermitsAndTimeout() {
        boolean result = limiter.acquire(2, 100, TimeUnit.MILLISECONDS);
        Assertions.assertTrue(result);
    }

    @Test
    void testAcquireWithPermitsAndTimeoutExceeded() {
        boolean result = limiter.acquire(1000, 10, TimeUnit.MILLISECONDS);
        Assertions.assertTrue(result);
        result = limiter.acquire(1000, 1, TimeUnit.MILLISECONDS);
        Assertions.assertFalse(result);
    }
}
