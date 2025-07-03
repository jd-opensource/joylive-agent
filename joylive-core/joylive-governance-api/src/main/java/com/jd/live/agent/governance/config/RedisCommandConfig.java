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
import com.jd.live.agent.governance.policy.AccessMode;
import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class RedisCommandConfig {

    @Getter
    @Setter
    @Config("manage")
    private Set<String> manageCommands;

    @Getter
    @Setter
    @Config("readonly")
    private Set<String> readonlyCommands;

    private transient Map<String, AccessMode> commands;

    public AccessMode getAccessMode(String command) {
        AccessMode accessMode = command == null ? null : commands.get(command);
        return accessMode == null ? AccessMode.READ_WRITE : accessMode;
    }

    protected void initialize() {
        commands = new HashMap<>(128);
        if (manageCommands != null) {
            manageCommands.forEach(command -> commands.put(command, AccessMode.NONE));
        }
        if (readonlyCommands != null) {
            readonlyCommands.forEach(command -> commands.put(command, AccessMode.READ_WRITE));
        }
    }

}

