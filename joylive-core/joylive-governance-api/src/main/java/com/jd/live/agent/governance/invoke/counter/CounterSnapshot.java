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

/**
 * A class that represents a snapshot of the Counter class's state at a specific point in time. It allows you to
 * calculate the estimated response time for the system based on the current number of active requests and the
 * average elapsed time for successful requests.
 */
public class CounterSnapshot {

    /**
     * The Counter instance from which this snapshot was taken.
     */
    private final Counter counter;
    /**
     * The offset of the number of successful requests at the time this snapshot was taken.
     */
    private final long succeededOffset;
    /**
     * The offset of the total elapsed time for successful requests at the time this snapshot was taken.
     */
    private final long succeededElapsedOffset;

    private final long lastSucceededAverageElapsed;

    public CounterSnapshot(Counter counter) {
        this(counter, 0);
    }

    public CounterSnapshot(final Counter counter, final long lastSucceededAverageElapsed) {
        this.counter = counter;
        this.succeededOffset = counter.getSucceeded();
        this.succeededElapsedOffset = counter.getSucceededElapsed();
        this.lastSucceededAverageElapsed = lastSucceededAverageElapsed;
    }

    public long getSucceededAverageElapsed() {
        long succeed = this.counter.getSucceeded() - this.succeededOffset;
        if (succeed == 0) {
            return lastSucceededAverageElapsed;
        }
        return (this.counter.getSucceededElapsed() - this.succeededElapsedOffset) / succeed;
    }

    public long getEstimateResponse() {
        int active = this.counter.getActive() + 1;
        return getSucceededAverageElapsed() * active;
    }
}
