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
package com.jd.live.agent.core.config;

import lombok.Getter;
import lombok.Setter;

/**
 * Configuration class for a timer.
 */
@Getter
@Setter
public class TimerConfig {

    /**
     * The time interval for each tick in milliseconds.
     */
    private long tickTime = 200;

    /**
     * The number of ticks.
     */
    private int ticks = 300;

    /**
     * The number of worker threads.
     */
    private int workerThreads = 4;

    /**
     * The maximum number of tasks that can be handled.
     */
    private long maxTasks = 0;
}
