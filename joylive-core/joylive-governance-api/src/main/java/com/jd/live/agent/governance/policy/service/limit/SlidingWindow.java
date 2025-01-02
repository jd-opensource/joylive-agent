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
package com.jd.live.agent.governance.policy.service.limit;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * Represents a sliding window for rate limiting purposes. A sliding window defines a period of time
 * (time window) and a threshold, which is the maximum number of allowed actions (e.g., requests) within
 * that time window. This class is typically used in conjunction with rate limiting policies to enforce
 * limits on the frequency of certain operations.
 * <p>
 * The sliding window mechanism helps in evenly distributing allowed actions over time, preventing bursts
 * of actions in short periods and thus ensuring fair usage and system stability.
 * </p>
 *
 * @since 1.0.0
 */
public class SlidingWindow implements Serializable {

    private static final int DEFAULT_PERMITS = 1000;

    private static final long DEFAULT_TIME_WINDOW = 1000L;

    /**
     * The maximum number of allowed actions within the time window.
     */
    @Getter
    @Setter
    private int threshold;

    /**
     * The duration of the time window in milliseconds.
     */
    @Getter
    @Setter
    private long timeWindowInMs;

    private volatile transient Double permitIntervalMicros;

    /**
     * Default constructor for creating an instance without initializing fields.
     */
    public SlidingWindow() {
        this(DEFAULT_PERMITS, DEFAULT_TIME_WINDOW);
    }

    /**
     * Constructs a new sliding window with the specified threshold and time window duration.
     *
     * @param threshold     the maximum number of allowed actions within the time window
     * @param timeWindowInMs the duration of the time window in milliseconds
     */
    public SlidingWindow(int threshold, long timeWindowInMs) {
        this.threshold = threshold <= 0 ? DEFAULT_PERMITS : threshold;
        this.timeWindowInMs = timeWindowInMs <= 0 ? DEFAULT_TIME_WINDOW : timeWindowInMs;
    }

    /**
     * Returns the time interval (in microseconds) between each permit.
     *
     * @return The time interval (in microseconds) between each permit.
     */
    public double getPermitIntervalMicros() {
        if (permitIntervalMicros == null) {
            synchronized (this) {
                if (permitIntervalMicros == null) {
                    double timeWindowMicros = (timeWindowInMs <= 0 ? DEFAULT_TIME_WINDOW : timeWindowInMs) * 1000D;
                    long maxRequests = threshold <= 0 ? DEFAULT_PERMITS : threshold;
                    permitIntervalMicros = timeWindowMicros / maxRequests;
                }
            }
        }
        return permitIntervalMicros;
    }
}

