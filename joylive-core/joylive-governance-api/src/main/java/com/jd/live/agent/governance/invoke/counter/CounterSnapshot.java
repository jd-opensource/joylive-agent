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

public class CounterSnapshot {

    private final Counter counter;
    private final long succeededOffset;
    private final long succeededElapsedOffset;

    public CounterSnapshot(Counter counter) {
        this.counter = counter;
        this.succeededOffset = counter.getSucceeded();
        this.succeededElapsedOffset = counter.getSucceededElapsed();
    }

    private long getSucceededAverageElapsed() {
        long succeed = this.counter.getSucceeded() - this.succeededOffset;
        if (succeed == 0) {
            return 0;
        }
        return (this.counter.getSucceededElapsed() - this.succeededElapsedOffset) / succeed;
    }

    public long getEstimateResponse() {
        int active = this.counter.getActive() + 1;
        return getSucceededAverageElapsed() * active;
    }
}
