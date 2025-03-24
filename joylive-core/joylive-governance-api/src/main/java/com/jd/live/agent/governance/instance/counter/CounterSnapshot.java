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
package com.jd.live.agent.governance.instance.counter;

/**
 * An interface that provides methods to retrieve statistical snapshots of a {@link Counter}.
 * Implementations of this interface are responsible for providing metrics such as average elapsed time,
 * success counts, and estimated response times based on the underlying counter data.
 */
public interface CounterSnapshot {

    /**
     * Returns the average elapsed time for succeeded operations.
     *
     * @return the average elapsed time in milliseconds (or other relevant unit)
     */
    long getSucceededAverageElapsed();

    /**
     * Returns the average elapsed time for succeeded operations, calculated based on the provided
     * number of successful operations.
     *
     * @param succeed the number of successful operations to use for the calculation
     * @return the average elapsed time in milliseconds (or other relevant unit)
     */
    long getSucceededAverageElapsed(long succeed);

    /**
     * Returns the total number of succeeded operations.
     *
     * @return the count of succeeded operations
     */
    long getSucceeded();

    /**
     * Returns the estimated response time based on the current snapshot data.
     *
     * @return the estimated response time in milliseconds (or other relevant unit)
     */
    long getEstimateResponse();

    /**
     * Returns the underlying {@link Counter} instance associated with this snapshot.
     *
     * @return the associated {@link Counter} instance
     */
    Counter getCounter();
}

