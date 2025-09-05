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
package com.jd.live.agent.core.util.timer;

import com.jd.live.agent.core.util.time.TimeScheduler;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class TimerTest {

    @Test
    public void schedule() throws InterruptedException {
        TimeScheduler timer = new TimeScheduler("LiveAgent-timer", 200, 300, 2, 0);
        timer.start();
        AtomicReference<Counter> ref = new AtomicReference<>(new Counter(0, 0));
        final long start = System.currentTimeMillis();
        // interval must greater than tickTime
        timer.schedule("test", 200, 100, () -> {
            Counter counter = ref.get();
            ref.set(new Counter(System.currentTimeMillis() - start, counter.count + 1));
        });

        CountDownLatch countDownLatch = new CountDownLatch(1);
        countDownLatch.await(1000, TimeUnit.MILLISECONDS);
        Counter counter = ref.get();
        double avgTime = counter.elapseTime * 1.0 / counter.count;
        System.out.println("count:" + counter.count + ", average:" + avgTime);
        Assertions.assertTrue(counter.count >= 3);
        Assertions.assertTrue(avgTime >= 200.0);
    }

    private static class Counter {
        long elapseTime;
        long count;

        public Counter(long elapseTime, long count) {
            this.elapseTime = elapseTime;
            this.count = count;
        }
    }
}
