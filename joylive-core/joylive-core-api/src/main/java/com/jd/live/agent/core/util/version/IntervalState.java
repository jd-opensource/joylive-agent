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
package com.jd.live.agent.core.util.version;

/**
 * Enum representing interval states, indicating the openness or closedness of interval ends.
 * An interval can be left-closed-right-closed ([a, b]), left-closed-right-open ([a, b)),
 * left-open-right-closed ((a, b]), or left-open-right-open ((a, b)).
 */
public enum IntervalState {
    /**
     * Interval where both ends are closed.
     */
    LEFT_CLOSE_RIGHT_CLOSE,

    /**
     * Interval where the left end is closed and the right end is open.
     */
    LEFT_CLOSE_RIGHT_OPEN,

    /**
     * Interval where the left end is open and the right end is closed.
     */
    LEFT_OPEN_RIGHT_CLOSE,

    /**
     * Interval where both ends are open.
     */
    LEFT_OPEN_RIGHT_OPEN;
}

