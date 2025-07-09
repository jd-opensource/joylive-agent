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
package com.jd.live.agent.core.util.shutdown;

import java.util.List;

public interface GracefullyShutdown {

    int DEFAULT_SHUTDOWN_WAIT_TIME = 10 * 1000;

    /**
     * Gets the wait time in milliseconds before processing.
     * Default implementation returns 0 (no waiting).
     *
     * @return wait time in milliseconds (0 by default)
     */
    default int getWaitTime() {
        return 0;
    }

    /**
     * Finds the maximum wait time among all graceful shutdown instances.
     *
     * @param shutdowns List of shutdown objects to check
     * @return Maximum wait time in milliseconds (0 if none found)
     */
    static int getMaxWaitTime(List<?> shutdowns) {
        int maxWaitTime = 0;
        for (Object shutdown : shutdowns) {
            if (shutdown instanceof GracefullyShutdown) {
                maxWaitTime = Math.max(maxWaitTime, ((GracefullyShutdown) shutdown).getWaitTime());
            }
        }
        return maxWaitTime;
    }

    /**
     * Gets the maximum wait time among graceful shutdowns, falling back to default if none found.
     *
     * @param shutdowns       List of shutdown objects to check
     * @param defaultWaitTime Fallback value if no valid wait times found (in milliseconds)
     * @return Maximum wait time in milliseconds, or default if none found/valid
     */
    static int getMaxWaitTime(List<?> shutdowns, int defaultWaitTime) {
        int maxWaitTime = getMaxWaitTime(shutdowns);
        return maxWaitTime <= 0 ? defaultWaitTime : maxWaitTime;
    }
}
