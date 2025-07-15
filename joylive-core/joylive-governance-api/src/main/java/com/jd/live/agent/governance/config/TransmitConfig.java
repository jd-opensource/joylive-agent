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
package com.jd.live.agent.governance.config;

import com.jd.live.agent.bootstrap.util.Inclusion;
import com.jd.live.agent.core.inject.annotation.Config;
import com.jd.live.agent.governance.context.bag.AutoDetect;
import lombok.Getter;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

public class TransmitConfig {

    /**
     * A collection of keys that need to be transmitted.
     */
    @Getter
    @Setter
    private Set<String> keys;

    /**
     * A collection of key prefixes that need to be transmitted.
     */
    @Getter
    @Setter
    private Set<String> prefixes;

    /**
     * Transmit type, W3cBaggage as the default selection
     */
    @Getter
    @Setter
    private String type = "w3c";

    /**
     * Auto-detect transmit type when reading.
     */
    @Getter
    @Setter
    private AutoDetect autoDetect = AutoDetect.NONE;

    /**
     * Thread transmit config
     */
    @Config("thread")
    @Getter
    @Setter
    private ThreadConfig threadConfig = new ThreadConfig();

    private transient Inclusion inclusion;

    public boolean include(String key) {
        return inclusion.test(key);
    }

    protected void initialize() {
        inclusion = new Inclusion(keys, prefixes);
        threadConfig.initialize();
    }

    public static class ThreadConfig {

        @Getter
        @Setter
        private Set<String> excludeExecutors = new HashSet<>();

        @Getter
        @Setter
        private Set<String> excludeExecutorPrefixes = new HashSet<>();

        @Getter
        @Setter
        private Set<String> excludeTasks = new HashSet<>();

        @Getter
        @Setter
        private Set<String> excludeTaskPrefixes = new HashSet<>();

        private transient Inclusion executorInclusion;

        private transient Inclusion taskInclusion;

        /**
         * Checks if the given executor type is excluded.
         *
         * @param type The class type of the executor to check.
         * @return {@code true} if the executor type is excluded, {@code false} otherwise.
         */
        public boolean isExcludedExecutor(Class<?> type) {
            return type != null && isExcludedExecutor(type.getName());
        }

        /**
         * Checks if the given executor name is excluded.
         *
         * @param name The name of the executor to check.
         * @return {@code true} if the executor name is excluded, {@code false} otherwise.
         */
        public boolean isExcludedExecutor(String name) {
            return executorInclusion.test(name);
        }

        /**
         * Checks if the given task type is excluded.
         *
         * @param type The class type of the task to check.
         * @return {@code true} if the task type is excluded, {@code false} otherwise.
         */
        public boolean isExcludedTask(Class<?> type) {
            return type != null && isExcludedTask(type.getName());
        }

        /**
         * Checks if the given task name is excluded.
         *
         * @param name The name of the task to check.
         * @return {@code true} if the task name is excluded or matches any excluded prefix, {@code false} otherwise.
         */
        public boolean isExcludedTask(String name) {
            return taskInclusion.test(name);
        }

        protected void initialize() {
            executorInclusion = new Inclusion(excludeExecutors, excludeExecutorPrefixes);
            taskInclusion = new Inclusion(excludeTasks, excludeTaskPrefixes);
        }

    }

}

