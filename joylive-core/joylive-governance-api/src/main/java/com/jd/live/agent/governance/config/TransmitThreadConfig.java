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

import lombok.Getter;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
public class TransmitThreadConfig {

    public static final String CONFIG_THREAD_PREFIX = "agent.governance.transmission.thread";

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
