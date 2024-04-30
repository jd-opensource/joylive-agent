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
package com.jd.live.agent.core.command;

import com.jd.live.agent.core.extension.annotation.Extensible;

import java.util.Map;

/**
 * Represents a functional interface for executing commands within a system.
 * This interface is marked as extensible with the "Command" designation, indicating
 * that it can be implemented to provide various command execution behaviors. It is
 * also marked as a functional interface, implying that it is intended for implementations
 * that can be expressed as lambda expressions or method references.
 * <p>
 * Commands defined within this interface, such as installing or uninstalling plugins,
 * or unloading an agent, are represented as constants. The {@code execute} method
 * provides a generic way to execute these commands based on a set of arguments.
 */
@Extensible("Command")
@FunctionalInterface
public interface Command {

    /**
     * Command string for installing a plugin.
     */
    String CMD_INSTALL_PLUGIN = "install";

    /**
     * Command string for uninstalling a plugin.
     */
    String CMD_UNINSTALL_PLUGIN = "uninstall";

    /**
     * Command string for unloading an agent.
     */
    String CMD_UNLOAD_AGENT = "unload";

    /**
     * Argument key for specifying a plugin.
     */
    String ARG_PLUGIN = "plugin";

    /**
     * Executes a command based on the provided arguments.
     * Implementations of this method should interpret the arguments to determine
     * the specific command to execute and carry out the corresponding actions.
     * The nature of these actions can vary widely depending on the specific command
     * and the context in which it is executed.
     *
     * @param args A map containing the arguments needed to execute the command.
     *             Keys in this map represent argument names, and values represent
     *             the corresponding argument values.
     */
    void execute(Map<String, Object> args);
}

