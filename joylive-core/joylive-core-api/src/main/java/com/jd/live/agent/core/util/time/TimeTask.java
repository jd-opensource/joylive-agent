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

/**
 * Represents a task that can be executed at a specific time.
 */
public interface TimeTask extends Runnable {

    /**
     * Retrieves the name of the task.
     *
     * @return A {@code String} representing the name of the task.
     */
    String getName();

    /**
     * Retrieves the scheduled execution time for the task. The time is expected to be a specific
     * point in time, represented as a long value, such as a timestamp.
     *
     * @return A {@code long} value representing the scheduled execution time of the task.
     */
    long getTime();
}

