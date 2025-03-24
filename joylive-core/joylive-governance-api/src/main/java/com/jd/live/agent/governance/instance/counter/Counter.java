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
 * An interface that provides methods to track and measure the performance and status of operations.
 * Implementations of this interface are responsible for recording metrics such as success/failure counts,
 * elapsed times, active operations, and throughput, as well as generating snapshots of the current state.
 */
public interface Counter {

    /**
     * Begins tracking a new operation, ensuring the number of active operations does not exceed the specified maximum.
     *
     * @param max the maximum number of active operations allowed
     * @return {@code true} if the operation can begin, {@code false} if the maximum active operations limit is reached
     */
    boolean begin(int max);

    /**
     * Records a successful operation with the specified elapsed time.
     *
     * @param elapsed the elapsed time of the operation in milliseconds (or other relevant unit)
     */
    void success(long elapsed);

    /**
     * Records a failed operation with the specified elapsed time.
     *
     * @param elapsed the elapsed time of the operation in milliseconds (or other relevant unit)
     */
    void fail(long elapsed);

    /**
     * Ends an operation, recording its elapsed time and success status.
     *
     * @param elapsed   the elapsed time of the operation in milliseconds (or other relevant unit)
     * @param succeeded {@code true} if the operation succeeded, {@code false} otherwise
     */
    void end(long elapsed, boolean succeeded);

    /**
     * Returns the number of currently active operations.
     *
     * @return the count of active operations
     */
    int getActive();

    /**
     * Returns the total number of operations recorded (both succeeded and failed).
     *
     * @return the total count of operations
     */
    long getTotal();

    /**
     * Returns the total elapsed time for all operations recorded.
     *
     * @return the total elapsed time in milliseconds (or other relevant unit)
     */
    long getTotalElapsed();

    /**
     * Returns the average elapsed time for all operations recorded.
     *
     * @return the average elapsed time in milliseconds (or other relevant unit)
     */
    long getAverageElapsed();

    /**
     * Returns the maximum elapsed time for all operations recorded.
     *
     * @return the maximum elapsed time in milliseconds (or other relevant unit)
     */
    long getMaxElapsed();

    /**
     * Returns the total number of failed operations recorded.
     *
     * @return the count of failed operations
     */
    int getFailed();

    /**
     * Returns the total elapsed time for all failed operations recorded.
     *
     * @return the total elapsed time for failed operations in milliseconds (or other relevant unit)
     */
    long getFailedElapsed();

    /**
     * Returns the average elapsed time for all failed operations recorded.
     *
     * @return the average elapsed time for failed operations in milliseconds (or other relevant unit)
     */
    long getFailedAverageElapsed();

    /**
     * Returns the maximum elapsed time for all failed operations recorded.
     *
     * @return the maximum elapsed time for failed operations in milliseconds (or other relevant unit)
     */
    long getFailedMaxElapsed();

    /**
     * Returns the total number of succeeded operations recorded.
     *
     * @return the count of succeeded operations
     */
    long getSucceeded();

    /**
     * Returns the total elapsed time for all succeeded operations recorded.
     *
     * @return the total elapsed time for succeeded operations in milliseconds (or other relevant unit)
     */
    long getSucceededElapsed();

    /**
     * Returns the average elapsed time for all succeeded operations recorded.
     *
     * @return the average elapsed time for succeeded operations in milliseconds (or other relevant unit)
     */
    long getSucceededAverageElapsed();

    /**
     * Returns the maximum elapsed time for all succeeded operations recorded.
     *
     * @return the maximum elapsed time for succeeded operations in milliseconds (or other relevant unit)
     */
    long getSucceededMaxElapsed();

    /**
     * Returns the average throughput (operations per second) based on the recorded data.
     *
     * @return the average throughput in operations per second
     */
    long getAverageTps();

    /**
     * Returns a snapshot of the current state of the counter, capturing all recorded metrics.
     *
     * @return a {@link CounterSnapshot} instance representing the current state
     */
    CounterSnapshot getSnapshot();

    /**
     * Captures a snapshot of the current state of the counter and updates internal metrics.
     */
    void snapshot();

    EndpointCounter getParent();
}
