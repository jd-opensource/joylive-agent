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
package com.jd.live.agent.bootstrap.option;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.UnixStyleUsageFormatter;

/**
 * The {@code OptionParser} class provides a method to parse command-line arguments
 * into an {@link AgentOption} object using the JCommander library.
 */
public class OptionParser {

    /**
     * Parses the given command-line arguments into an {@link AgentOption} object.
     * If the help option is specified, it prints the usage information and returns {@code null}.
     *
     * @param args the command-line arguments to parse
     * @return the parsed {@link AgentOption} object, or {@code null} if help is requested
     */
    public static AgentOption parse(String[] args) {
        AgentOption option = new AgentOption();
        JCommander commander = JCommander.newBuilder().addObject(option).build();
        commander.setUsageFormatter(new UnixStyleUsageFormatter(commander));
        commander.parse(args);
        if (option.isHelp()) {
            commander.usage();
            return null;
        }
        return option;
    }
}