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

import com.jd.live.agent.governance.invoke.ratelimit.leakybucket.LeakyBucketLimiter;
import com.jd.live.agent.governance.policy.service.limit.RateLimitPolicy;
import com.jd.live.agent.governance.policy.service.limit.SlidingWindow;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * LeakyBucketLimiterTest
 *
 * @author Zhiguo.Chen
 * @since 1.0.0
 */
public class LeakyBucketLimiterTest {

    private RateLimitPolicy createPolicy(long maxWaitMs, int capacity) {
        RateLimitPolicy policy = new RateLimitPolicy();
        policy.setRealizeType("LeakyBucket");
        policy.setMaxWaitMs(maxWaitMs);
        Map<String, String> parameters = new HashMap<>();
        parameters.put("capacity", String.valueOf(capacity));
        policy.setParameters(parameters);
        return policy;
    }

    @Test
    public void testSmoothRate() throws InterruptedException {
        // Rate: 10 permits/sec, so 1 permit every 100ms.
        RateLimitPolicy policy = createPolicy(10000L, 100);
        LeakyBucketLimiter limiter = new LeakyBucketLimiter(policy, new SlidingWindow(10, 1000L));
        int totalRequests = 20;
        long startTime = System.currentTimeMillis();

        for (int i = 0; i < totalRequests; i++) {
            limiter.acquire();
        }

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        // Expected time for 20 requests at 10/sec is ~1900ms (the first one is free, the next 19 wait 100ms each).
        Assertions.assertTrue(duration >= 1900 && duration < 2100, "Requests should be processed at a smooth rate. Duration: " + duration);
    }

    @Test
    public void testBucketOverflow() throws InterruptedException {
        // Rate: 10 permits/sec. Capacity: 10.
        final int capacity = 10;
        final int threads = 20;
        RateLimitPolicy policy = createPolicy(1000L, capacity);
        final LeakyBucketLimiter limiter = new LeakyBucketLimiter(policy, new SlidingWindow(10, 1000L));
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        final CountDownLatch startGate = new CountDownLatch(1);
        final CountDownLatch endGate = new CountDownLatch(threads);
        final AtomicInteger successCount = new AtomicInteger(0);

        for (int i = 0; i < threads; i++) {
            executor.submit(() -> {
                try {
                    startGate.await(); // Wait for all threads to be ready
                    if (limiter.acquire()) {
                        successCount.incrementAndGet();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    endGate.countDown();
                }
            });
        }

        startGate.countDown(); // Open the gate
        endGate.await(); // Wait for all threads to finish
        executor.shutdown();

        // With the race condition, it's likely that more than 'capacity' requests will succeed.
        // A correct implementation should grant around 'capacity' permits.
        // The original implementation will likely fail this stricter assertion.
        Assertions.assertTrue(successCount.get() <= capacity,
                "Should reject requests when bucket overflows. Success count: " + successCount.get());
    }

    @Test
    public void testTimeout() {
        // Rate: 10 permits/sec (100ms per permit). Capacity: 100.
        // To acquire 10 permits, the 10th one needs to wait 900ms.
        RateLimitPolicy policy = createPolicy(800L, 100);
        LeakyBucketLimiter limiter = new LeakyBucketLimiter(policy, new SlidingWindow(10, 1000L));

        // This should fail because the required wait time (900ms) is greater than the timeout (800ms).
        Assertions.assertFalse(limiter.acquire(10));

        // This should succeed because the timeout is long enough.
        RateLimitPolicy successPolicy = createPolicy(1000L, 100);
        LeakyBucketLimiter successLimiter = new LeakyBucketLimiter(successPolicy, new SlidingWindow(10, 1000L));
        Assertions.assertTrue(successLimiter.acquire(10));
    }
}
