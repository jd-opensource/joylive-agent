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

import com.jd.live.agent.core.inject.annotation.Config;
import lombok.Getter;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
public class TransmitConfig {

    public static final String DEFAULT_PROPAGATION = "defaultPropagation";

    /**
     * A collection of keys that need to be transmitted.
     */
    private Set<String> keys;

    /**
     * A collection of key prefixes that need to be transmitted.
     */
    private Set<String> prefixes;

    /**
     * A collection of key suffixes that need to be transmitted.
     */
    private Set<String> suffixes;

    /**
     * Transmit type, W3cBaggage as the default selection
     */
    private String type = "W3cBaggage";

    /**
     * Thread transmit config
     */
    @Config("thread")
    private ThreadConfig threadConfig = new ThreadConfig();

    @Getter
    @Setter
    public static class ThreadConfig {

        private Set<String> excludeExecutors = new HashSet<>();

        private Set<String> excludeTasks = new HashSet<>();

        private Set<String> excludeTaskPrefixes = new HashSet<>();

        public boolean isExcludedExecutor(String name) {
            return name == null || excludeExecutors.contains(name);
        }

        public boolean isExcludedTask(String name) {
            return name == null || excludeTasks.contains(name) || isExcludedTaskPrefix(name);
        }

        protected boolean isExcludedTaskPrefix(String name) {
            if (name == null) {
                return false;
            }
            for (String prefix : excludeTaskPrefixes) {
                if (name.startsWith(prefix)) {
                    return true;
                }
            }
            return false;
        }
    }

}

