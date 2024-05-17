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
 * A {@code DelegateTask} is a concrete implementation of the {@code TimeTask} interface, encapsulating a task
 * with a specific execution time and a name. It delegates the execution to the {@code Runnable} provided at
 * construction time. This class can be used to wrap a {@code Runnable} with additional timing and identification
 * properties, making it suitable for scheduled execution in a timing framework.
 */
public class DelegateTask implements TimeTask {
    /**
     * The name of the task, used for identification purposes.
     */
    private final String name;

    /**
     * The scheduled execution time for the task, represented as a timestamp.
     */
    private final long time;

    /**
     * The {@code Runnable} containing the code to be executed when the task runs.
     */
    private final Runnable runnable;

    /**
     * Constructs a new {@code DelegateTask} with the specified name, time, and executable code.
     *
     * @param name     The name of the task.
     * @param time     The execution time of the task as a timestamp.
     * @param runnable The {@code Runnable} to be executed.
     */
    public DelegateTask(final String name, final long time, final Runnable runnable) {
        this.name = name;
        this.time = time;
        this.runnable = runnable;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public long getTime() {
        return time;
    }

    @Override
    public void run() {
        if (runnable != null) {
            runnable.run();
        }
    }
}

