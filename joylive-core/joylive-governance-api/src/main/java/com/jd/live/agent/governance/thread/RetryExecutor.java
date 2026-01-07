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
package com.jd.live.agent.governance.thread;

import com.jd.live.agent.core.extension.annotation.Extensible;

import java.util.concurrent.TimeUnit;

/**
 * Retry executor interface for managing retry operations.
 * Provides a simple interface for submitting retry tasks with optional delay.
 */
@Extensible("RetryExecutor")
public interface RetryExecutor {

    /**
     * Submits a retry task for immediate execution.
     *
     * @param task the retry task to execute
     */
    void submit(RetryTask task);

    /**
     * Submits a retry task for execution with delay.
     *
     * @param task  the retry task to execute
     * @param delay the delay time
     * @param unit  the time unit of the delay
     */
    void submit(RetryTask task, long delay, TimeUnit unit);

    /**
     * Retry task interface for defining retry operations.
     */
    @FunctionalInterface
    interface RetryTask {

        /**
         * Executes the retry operation.
         */
        void execute();
    }
}