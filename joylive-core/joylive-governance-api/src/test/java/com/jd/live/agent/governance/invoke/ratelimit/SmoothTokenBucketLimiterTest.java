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
import com.jd.live.agent.governance.invoke.ratelimit.tokenbucket.SmoothWarmupLimiter;
import com.jd.live.agent.governance.policy.service.limit.RateLimitPolicy;
import com.jd.live.agent.governance.policy.service.limit.SlidingWindow;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

/**
 * SmoothTokenBucketLimiterTest
 *
 * @author Zhiguo.Chen
 * @since 1.0.0
 */
public class SmoothTokenBucketLimiterTest {

    @Nested
    class SmoothBurstyLimiterTest {
        private RateLimitPolicy createPolicy(long maxWaitMs, int maxBurstSeconds) {
            RateLimitPolicy policy = new RateLimitPolicy();
            policy.setRealizeType("SmoothBursty");
            policy.setMaxWaitMs(maxWaitMs);
            Map<String, String> parameters = new HashMap<>();
            parameters.put("maxBurstSeconds", String.valueOf(maxBurstSeconds));
            policy.setParameters(parameters);
            return policy;
        }

        @Test
        void testAcquireWithSufficientPermits() {
            RateLimitPolicy policy = createPolicy(10000L, 5);
            SmoothBurstyLimiter limiter = new SmoothBurstyLimiter(policy, new SlidingWindow(10, 1000L));
            // Bucket has 50 permits initially. Acquiring 50 should succeed immediately.
            Assertions.assertTrue(limiter.acquire(50));
        }

        @Test
        void testAcquireWithTimeout_shouldSucceed() {
            RateLimitPolicy policy = createPolicy(10000L, 5);
            SmoothBurstyLimiter limiter = new SmoothBurstyLimiter(policy, new SlidingWindow(10, 1000L));
            // Acquire 60 permits. 50 are available, 10 are borrowed.
            // Wait time for 10 permits is 1000ms. Default timeout is 10000ms. Should succeed.
            Assertions.assertTrue(limiter.acquire(60));
        }

        @Test
        void testAcquireWithTimeoutExceeded_shouldFail() {
            RateLimitPolicy policy = createPolicy(900L, 5);
            SmoothBurstyLimiter limiter = new SmoothBurstyLimiter(policy, new SlidingWindow(10, 1000L));
            // Acquire 60 permits. Wait time is 1000ms. Default timeout is 900ms. Should fail.
            Assertions.assertFalse(limiter.acquire(60));
        }

        @Test
        void testBurstyLimiter_shouldCauseWait() {
            RateLimitPolicy policy = createPolicy(10000L, 5);
            SmoothBurstyLimiter limiter = new SmoothBurstyLimiter(policy, new SlidingWindow(10, 1000L));

            long startTime = System.currentTimeMillis();
            // Acquire 60 permits. 50 are available, 10 are borrowed.
            // Wait time for 10 permits is 10 * (1000ms / 10) = 1000ms.
            boolean result = limiter.acquire(60);
            long cost = System.currentTimeMillis() - startTime;

            Assertions.assertTrue(result);
            // Check if the cost is roughly 1000ms. Allow for some buffer.
            Assertions.assertTrue(cost >= 950 && cost < 1150, "Acquiring more permits than stored should cause a wait. Actual cost: " + cost);
        }

        @Test
        void testFastFail_whenMaxWaitIsZero() {
            RateLimitPolicy policy = createPolicy(0L, 5);
            SmoothBurstyLimiter limiter = new SmoothBurstyLimiter(policy, new SlidingWindow(10, 1000L));
            // Bucket has 50 permits. Try to acquire 51.
            // Since maxWaitMs is 0, it should fail immediately without waiting.
            Assertions.assertFalse(limiter.acquire(51));
        }
    }

    @Nested
    class SmoothWarmupLimiterTest {

        private RateLimitPolicy createPolicy(long maxWaitMs, int warmupSeconds, double coldFactor) {
            RateLimitPolicy policy = new RateLimitPolicy();
            policy.setRealizeType("SmoothWarmup");
            policy.setMaxWaitMs(maxWaitMs);
            Map<String, String> parameters = new HashMap<>();
            parameters.put("warmupSeconds", String.valueOf(warmupSeconds));
            parameters.put("coldFactor", String.valueOf(coldFactor));
            policy.setParameters(parameters);
            return policy;
        }

        @Test
        void testColdAcquire() {
            RateLimitPolicy policy = createPolicy(10000L, 5, 3.0);
            SmoothWarmupLimiter limiter = new SmoothWarmupLimiter(policy, new SlidingWindow(10, 1000L));
            // Rate is 10 permits/sec, so stable interval is 100ms.
            // Cold factor is 3, so the coldest interval is ~300ms.

            // The first acquire in a cold limiter is the most expensive.
            long cost1 = costOfAcquire(limiter, 1);
            Assertions.assertTrue(cost1 >= 280 && cost1 < 320, "First acquire in a cold limiter should be at the coldest rate. Cost: " + cost1);

            // The second acquire should be slightly cheaper.
            long cost2 = costOfAcquire(limiter, 1);
            Assertions.assertTrue(cost2 < cost1, "Second acquire should be cheaper than the first.");
        }

        @Test
        void testWarmup() {
            RateLimitPolicy policy = createPolicy(10000L, 2, 3.0);
            SmoothWarmupLimiter limiter = new SmoothWarmupLimiter(policy, new SlidingWindow(10, 1000L));
            // Stable interval: 100ms, Cold interval: 300ms

            // Subsequent acquires should get progressively faster.
            long lastCost = costOfAcquire(limiter, 1); // First one is the most expensive
            for (int i = 0; i < 10; i++) {
                long currentCost = costOfAcquire(limiter, 1);
                Assertions.assertTrue(currentCost <= lastCost, "Acquire cost should decrease or stay the same during warmup.");
                lastCost = currentCost;
            }
        }

        @Test
        void testStableState() {
            RateLimitPolicy policy = createPolicy(10000L, 2, 3.0);
            SmoothWarmupLimiter limiter = new SmoothWarmupLimiter(policy, new SlidingWindow(10, 1000L));
            // Stable interval: 100ms

            // Warm up the limiter by acquiring enough permits
            for (int i = 0; i < 30; i++) {
                limiter.acquire(1);
            }

            // Now in stable state, cost should be around 100ms
            long cost = costOfAcquire(limiter, 1);
            Assertions.assertTrue(cost >= 90 && cost < 110, "Acquire cost in stable state should be at stable rate. Cost: " + cost);
        }

        private long costOfAcquire(RateLimiter limiter, int permits) {
            long startTime = System.currentTimeMillis();
            limiter.acquire(permits);
            return System.currentTimeMillis() - startTime;
        }
    }
}