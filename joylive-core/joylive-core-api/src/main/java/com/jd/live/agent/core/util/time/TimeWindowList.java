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

import java.util.ArrayList;
import java.util.List;

/**
 * A class that manages a list of TimeWindow objects.
 */
public class TimeWindowList {

    /**
     * The first TimeWindow added to the list.
     */
    private TimeWindow window;

    /**
     * A list to store multiple TimeWindow objects.
     */
    private List<TimeWindow> windows;

    /**
     * Constructs a new TimeWindowList.
     */
    public TimeWindowList() {
    }

    /**
     * Adds a TimeWindow to the list.
     *
     * @param window The TimeWindow to add.
     */
    public void add(TimeWindow window) {
        if (window != null) {
            if (this.window == null) {
                this.window = window;
            } else if (windows == null) {
                windows = new ArrayList<>();
                windows.add(this.window);
                windows.add(window);
            } else {
                windows.add(window);
            }
        }
    }

    /**
     * Returns the TimeWindow with the maximum end time from the list.
     * If there are multiple TimeWindows, it returns a new TimeWindow
     * with the maximum start time and maximum end time from the list.
     *
     * @return The TimeWindow with the maximum start and end time.
     */
    public TimeWindow max() {
        if (windows == null) {
            return window;
        }
        long maxStartTime = Long.MIN_VALUE;
        long maxEndTime = Long.MIN_VALUE;
        for (TimeWindow window : windows) {
            if (window.startTime > maxStartTime) {
                maxStartTime = window.startTime;
            }
            if (window.endTime > maxEndTime) {
                maxEndTime = window.endTime;
            }
        }
        return new TimeWindow(maxStartTime, (int) (maxEndTime - maxStartTime));
    }
}
