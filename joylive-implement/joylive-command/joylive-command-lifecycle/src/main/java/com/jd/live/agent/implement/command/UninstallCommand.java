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
package com.jd.live.agent.implement.command;

import com.jd.live.agent.core.command.Command;
import com.jd.live.agent.core.extension.annotation.Extension;
import com.jd.live.agent.core.inject.annotation.Inject;
import com.jd.live.agent.core.plugin.PluginSupervisor;
import com.jd.live.agent.core.util.StringUtils;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;

@Extension(Command.CMD_UNINSTALL_PLUGIN)
public class UninstallCommand implements Command {
    @Inject
    private PluginSupervisor pluginSupervisor;

    public UninstallCommand() {
    }

    public UninstallCommand(PluginSupervisor pluginSupervisor) {
        this.pluginSupervisor = pluginSupervisor;
    }

    @Override
    public void execute(Map<String, Object> args) {
        String config = (String) args.get(Command.ARG_PLUGIN);
        if (config != null && !config.isEmpty()) {
            pluginSupervisor.uninstall(new HashSet<>(Arrays.asList(StringUtils.split(config, ','))));
        }
    }
}
