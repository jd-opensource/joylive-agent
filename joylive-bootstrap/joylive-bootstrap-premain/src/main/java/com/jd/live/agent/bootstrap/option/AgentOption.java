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

import com.beust.jcommander.DynamicParameter;
import com.beust.jcommander.Parameter;
import com.jd.live.agent.bootstrap.LivePath;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.util.HashMap;
import java.util.Map;

/**
 * The {@code AgentOption} class represents the command-line options for loading a Java agent.
 * It includes options for specifying the target JVM process ID, the agent path, agent arguments, and other settings.
 */
public class AgentOption {

    @Parameter(names = {"-p", "-pid"}, description = "The target jvm process id")
    private String pid;

    @Parameter(names = {"-n", "-name"}, description = "The target jvm process name")
    private String name;

    @Parameter(names = {"-t", "-path"}, description = "The agent root path")
    private String agentPath;

    @DynamicParameter(names = {"-a", "-arg"}, description = "The agent argument")
    private Map<String, String> args;

    @Parameter(names = {"-h", "-help"}, help = true, description = "The help information")
    private boolean help;

    @Parameter(names = {"-i", "-interactive"}, description = "The interactive mode")
    private boolean interactive = true;

    public String getPid() {
        return pid;
    }

    public void setPid(String pid) {
        this.pid = pid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAgentPath() {
        return agentPath;
    }

    public void setAgentPath(String agentPath) {
        this.agentPath = agentPath;
    }

    public Map<String, String> getArgs() {
        return args;
    }

    public void setArgs(Map<String, String> args) {
        this.args = args;
    }

    public void addArg(String key, String value) {
        if (key != null && !key.isEmpty() && value != null && !value.isEmpty()) {
            if (args == null) {
                args = new HashMap<>();
            }
            args.put(key, value);
        }
    }

    public boolean isHelp() {
        return help;
    }

    public void setHelp(boolean help) {
        this.help = help;
    }

    public boolean isInteractive() {
        return interactive;
    }

    public void setInteractive(boolean interactive) {
        this.interactive = interactive;
    }

    /**
     * Constructs the agent arguments as a single string.
     *
     * @return the agent arguments string
     */
    public String getAgentArgs() {
        StringBuilder sb = new StringBuilder();
        sb.append(LivePath.ARG_AGENT_PATH).append("=").append(agentPath);
        if (args != null && !args.isEmpty()) {
            args.forEach((k, v) -> sb.append(k).append("=").append(v).append(","));
        }
        return sb.toString();
    }

    /**
     * Validates the agent path by checking if the necessary files and directories exist.
     *
     * @param root the root directory to validate
     * @return {@code true} if the path is valid, {@code false} otherwise
     */
    public boolean isValidPath(File root) {
        if (root == null || !root.exists() || !root.isDirectory()) {
            return false;
        }
        File file = new File(root, LivePath.LIVE_JAR);
        File libDir = new File(root, LivePath.DIR_LIB);
        File configDir = new File(root, LivePath.DIR_CONFIG);
        return file.exists() && libDir.exists() && configDir.exists() && libDir.isDirectory() && configDir.isDirectory();
    }

    /**
     * Gets the current JVM process ID.
     *
     * @return the current process ID
     */
    public static String getJvmId() {
        String name = ManagementFactory.getRuntimeMXBean().getName();
        return name.split("@")[0];
    }
}