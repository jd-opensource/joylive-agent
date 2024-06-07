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
package com.jd.live.agent.core.bootstrap;

import java.util.Map;
import java.util.concurrent.Callable;

/**
 * Defines the lifecycle operations for an agent component within a system.
 * This interface allows for the management of an agent's lifecycle,
 * including its installation, uninstallation, and execution of specific commands.
 */
public interface AgentLifecycle {
    /**
     * The constant identifier for the agent lifecycle component.
     */
    String COMPONENT_AGENT_LIFECYCLE = "agentLifecycle";

    /**
     * Installs the agent, preparing it for operation within the system.
     * This method should handle any initialization or configuration required
     * to make the agent operational.
     */
    void install();

    /**
     * Uninstalls the agent, removing it from the system.
     * This method should clean up any resources used by the agent and ensure
     * that its removal does not negatively impact the system's operation.
     */
    void uninstall();

    /**
     * Executes a specific command on the agent.
     * This method allows for dynamic interaction with the agent by providing
     * a mechanism to invoke operations defined by the command and its arguments.
     *
     * @param command The command to execute, represented as a String.
     * @param args    A map containing the arguments required for the command execution.
     *                The keys represent argument names, and the values represent argument values.
     */
    void execute(String command, Map<String, Object> args);

    /**
     * Adds a hook that will be executed when the system is ready.
     * This can be used to perform initialization tasks or other operations
     * that should occur once the system has completed its startup process.
     *
     * @param callable the hook to be executed when the system is ready
     * @param classLoader execute in this classloader
     */
    void addReadyHook(Callable<?> callable, ClassLoader classLoader);
}
