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
package com.jd.live.agent.governance.counter.internal;

import com.jd.live.agent.core.util.AtomicUtils;
import com.jd.live.agent.governance.counter.Counter;
import com.jd.live.agent.governance.counter.EndpointCounter;
import lombok.Getter;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * A class used to track and monitor the number of active, total, failed, and successful requests, as well as the
 * elapsed time for each request. It also provides methods to calculate average elapsed time, maximum elapsed time,
 * and average transactions per second (TPS).
 * <p>
 * It's from org.apache.dubbo.rpc.RpcStatus
 */
public class InternalCounter implements Counter {

    /**
     * The number of active requests.
     */
    private final AtomicInteger active = new AtomicInteger();
    /**
     * The total number of requests.
     */
    private final AtomicLong total = new AtomicLong();
    /**
     * The number of failed requests.
     */
    private final AtomicInteger failed = new AtomicInteger();
    /**
     * The total elapsed time for all requests.
     */
    private final AtomicLong totalElapsed = new AtomicLong();
    /**
     * The total elapsed time for failed requests.
     */
    private final AtomicLong failedElapsed = new AtomicLong();
    /**
     * The maximum elapsed time for any request.
     */
    private final AtomicLong maxElapsed = new AtomicLong();
    /**
     * The maximum elapsed time for any failed request.
     */
    private final AtomicLong failedMaxElapsed = new AtomicLong();
    /**
     * The maximum elapsed time for any successful request.
     */
    private final AtomicLong succeededMaxElapsed = new AtomicLong();
    /**
     * The current snapshot of the counter's state.
     */
    private final AtomicReference<InternalCounterSnapshot> snapshot = new AtomicReference<>(new InternalCounterSnapshot(this));

    @Getter
    private final EndpointCounter parent;

    public InternalCounter(EndpointCounter parent) {
        this.parent = parent;
    }

    @Override
    public boolean begin(int max) {
        int maxValue = (max <= 0) ? Integer.MAX_VALUE : max;
        return AtomicUtils.increment(active, (older, newer) -> older != Integer.MAX_VALUE && newer <= maxValue);
    }

    @Override
    public void success(long elapsed) {
        end(elapsed, true);
    }

    @Override
    public void fail(long elapsed) {
        end(elapsed, false);
    }

    @Override
    public void end(long elapsed, boolean succeeded) {
        active.decrementAndGet();
        total.incrementAndGet();
        totalElapsed.addAndGet(elapsed);

        AtomicUtils.update(maxElapsed, elapsed, (older, newer) -> older < newer);
        if (succeeded) {
            AtomicUtils.update(succeededMaxElapsed, elapsed, (older, newer) -> older < newer);
        } else {
            failed.incrementAndGet();
            failedElapsed.addAndGet(elapsed);
            AtomicUtils.update(failedMaxElapsed, elapsed, (older, newer) -> older < newer);
        }
    }

    @Override
    public int getActive() {
        return active.get();
    }

    @Override
    public long getTotal() {
        return total.longValue();
    }

    @Override
    public long getTotalElapsed() {
        return totalElapsed.get();
    }

    @Override
    public long getAverageElapsed() {
        long total = getTotal();
        if (total == 0) {
            return 0;
        }
        return getTotalElapsed() / total;
    }

    @Override
    public long getMaxElapsed() {
        return maxElapsed.get();
    }

    @Override
    public int getFailed() {
        return failed.get();
    }

    @Override
    public long getFailedElapsed() {
        return failedElapsed.get();
    }

    @Override
    public long getFailedAverageElapsed() {
        long failed = getFailed();
        if (failed == 0) {
            return 0;
        }
        return getFailedElapsed() / failed;
    }

    @Override
    public long getFailedMaxElapsed() {
        return failedMaxElapsed.get();
    }

    @Override
    public long getSucceeded() {
        return getTotal() - getFailed();
    }

    @Override
    public long getSucceededElapsed() {
        return getTotalElapsed() - getFailedElapsed();
    }

    @Override
    public long getSucceededAverageElapsed() {
        long succeeded = getSucceeded();
        if (succeeded == 0) {
            return 0;
        }
        return getSucceededElapsed() / succeeded;
    }

    @Override
    public long getSucceededMaxElapsed() {
        return succeededMaxElapsed.get();
    }

    @Override
    public long getAverageTps() {
        if (getTotalElapsed() >= 1000L) {
            return getTotal() / (getTotalElapsed() / 1000L);
        }
        return getTotal();
    }

    @Override
    public InternalCounterSnapshot getSnapshot() {
        return snapshot.get();
    }

    @Override
    public void snapshot() {
        InternalCounterSnapshot last = snapshot.get();
        long succeeded = last.getSucceeded();
        long succeededAverageElapsed = succeeded < 10 ? 0 : last.getSucceededAverageElapsed(succeeded);
        snapshot.set(new InternalCounterSnapshot(this, succeededAverageElapsed));
    }
}
