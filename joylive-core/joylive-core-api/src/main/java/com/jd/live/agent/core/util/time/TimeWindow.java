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
package com.jd.live.agent.core.util.time;

import lombok.Getter;

/**
 * Represents a time window defined by a start time and a duration.
 * The end time is automatically calculated based on the start time and duration.
 */
@Getter
public class TimeWindow {

    /**
     * The start time of the time window.
     */
    protected final long startTime;

    /**
     * The end time of the time window, calculated as startTime + duration.
     */
    protected final long endTime;

    /**
     * The duration of the time window.
     */
    protected final long duration;

    /**
     * Constructs a new TimeWindow with the specified start time and duration.
     *
     * @param startTime The start time of the time window.
     * @param duration  The duration of the time window.
     */
    public TimeWindow(long startTime, long duration) {
        this.startTime = startTime;
        this.endTime = startTime + duration;
        this.duration = duration;
    }
}
