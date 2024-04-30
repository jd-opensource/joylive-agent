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
package com.jd.live.agent.implement.ratelimit.resilience4j.test;

import com.jd.live.agent.governance.policy.service.limit.RateLimitPolicy;
import com.jd.live.agent.governance.policy.service.limit.SlidingWindow;
import com.jd.live.agent.implement.flowcontrol.resilience4j.ratelimit.Resilience4jRateLimiter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.mockito.Mockito.mock;

/**
 * Resilience4jRateLimiterTest
 *
 * @since 1.0.0
 */
public class Resilience4jRateLimiterTest {

    private Resilience4jRateLimiter limiter;
    private RateLimitPolicy mockPolicy;

    @BeforeEach
    void setUp() {
        mockPolicy = mock(RateLimitPolicy.class);
        limiter = new Resilience4jRateLimiter(mockPolicy, new SlidingWindow(10, 1000L));
    }

    @ParameterizedTest
    @CsvSource({"1,true", "0,false", "-1,false", "1000,false"})
    void testAcquireWithPermits(int permits, boolean expected) {
        boolean result = limiter.acquire(permits);
        assert result == expected;
    }
}
