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
package com.jd.live.agent.core.util.task;

import com.jd.live.agent.core.util.time.Timer;
import lombok.Getter;

import java.util.function.Predicate;

/**
 * A versioned retry task that can be scheduled with a Timer.
 * Executes the task and reschedules it if needed based on a predicate.
 */
public class RetryVersionTimerTask implements RetryVersionTask {

    private final String name;

    private final RetryExecution task;

    @Getter
    private final long version;

    private final Predicate<RetryVersionTask> predicate;

    private final Timer timer;

    /**
     * Creates a new retry task.
     *
     * @param name      task identifier
     * @param task      the operation to retry
     * @param version   task version (used for validation)
     * @param predicate determines if retry should be attempted
     * @param timer     scheduler for retry attempts
     */
    public RetryVersionTimerTask(String name,
                                 RetryExecution task,
                                 long version,
                                 Predicate<RetryVersionTask> predicate,
                                 Timer timer) {
        this.name = name;
        this.task = task;
        this.version = version;
        this.predicate = predicate;
        this.timer = timer;
    }

    @Override
    public void run() {
        if (predicate.test(this)) {
            try {
                if (task.call()) {
                    return;
                }
                timer.delay(name, task.getRetryInterval(), this);
            } catch (Exception e) {
                timer.delay(name, task.getRetryInterval(), this);
            }
        }
    }

    @Override
    public void delay(long delay) {
        timer.delay(name, delay, this);
    }
}
