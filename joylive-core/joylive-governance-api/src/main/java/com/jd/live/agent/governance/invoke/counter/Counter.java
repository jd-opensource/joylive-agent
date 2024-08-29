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
package com.jd.live.agent.governance.invoke.counter;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

public class Counter {

    private final AtomicInteger active = new AtomicInteger();
    private final AtomicLong total = new AtomicLong();
    private final AtomicInteger failed = new AtomicInteger();
    private final AtomicLong totalElapsed = new AtomicLong();
    private final AtomicLong failedElapsed = new AtomicLong();
    private final AtomicLong maxElapsed = new AtomicLong();
    private final AtomicLong failedMaxElapsed = new AtomicLong();
    private final AtomicLong succeededMaxElapsed = new AtomicLong();

    private final AtomicReference<CounterSnapshot> snapshot = new AtomicReference<>(new CounterSnapshot(this));

    protected Counter() {
    }

    public boolean begin(int max) {
        max = (max <= 0) ? Integer.MAX_VALUE : max;
        if (active.get() == Integer.MAX_VALUE) {
            return false;
        }
        for (int i; ; ) {
            i = active.get();

            if (i == Integer.MAX_VALUE || i + 1 > max) {
                return false;
            }

            if (active.compareAndSet(i, i + 1)) {
                break;
            }
        }

        active.incrementAndGet();

        return true;
    }

    public void success(long elapsed) {
        end(elapsed, true);
    }

    public void fail(long elapsed) {
        end(elapsed, false);
    }

    public void end(long elapsed, boolean succeeded) {
        active.decrementAndGet();
        total.incrementAndGet();
        totalElapsed.addAndGet(elapsed);

        if (maxElapsed.get() < elapsed) {
            maxElapsed.set(elapsed);
        }

        if (succeeded) {
            if (succeededMaxElapsed.get() < elapsed) {
                succeededMaxElapsed.set(elapsed);
            }

        } else {
            failed.incrementAndGet();
            failedElapsed.addAndGet(elapsed);
            if (failedMaxElapsed.get() < elapsed) {
                failedMaxElapsed.set(elapsed);
            }
        }
    }

    public int getActive() {
        return active.get();
    }

    public long getTotal() {
        return total.longValue();
    }

    public long getTotalElapsed() {
        return totalElapsed.get();
    }

    public long getAverageElapsed() {
        long total = getTotal();
        if (total == 0) {
            return 0;
        }
        return getTotalElapsed() / total;
    }

    public long getMaxElapsed() {
        return maxElapsed.get();
    }

    public int getFailed() {
        return failed.get();
    }

    public long getFailedElapsed() {
        return failedElapsed.get();
    }

    public long getFailedAverageElapsed() {
        long failed = getFailed();
        if (failed == 0) {
            return 0;
        }
        return getFailedElapsed() / failed;
    }

    public long getFailedMaxElapsed() {
        return failedMaxElapsed.get();
    }

    public long getSucceeded() {
        return getTotal() - getFailed();
    }

    public long getSucceededElapsed() {
        return getTotalElapsed() - getFailedElapsed();
    }

    public long getSucceededAverageElapsed() {
        long succeeded = getSucceeded();
        if (succeeded == 0) {
            return 0;
        }
        return getSucceededElapsed() / succeeded;
    }

    public long getSucceededMaxElapsed() {
        return succeededMaxElapsed.get();
    }

    public long getAverageTps() {
        if (getTotalElapsed() >= 1000L) {
            return getTotal() / (getTotalElapsed() / 1000L);
        }
        return getTotal();
    }

    public CounterSnapshot getSnapshot() {
        return snapshot.get();
    }

    public void snapshot() {
        snapshot.set(new CounterSnapshot(this));
    }
}
