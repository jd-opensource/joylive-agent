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
package com.jd.live.agent.bootstrap;

import com.jd.live.agent.bootstrap.option.AgentOption;
import com.jd.live.agent.bootstrap.option.OptionParser;
import com.jd.live.agent.bootstrap.vm.VirtualMachineFactory;
import com.sun.tools.attach.VirtualMachine;
import com.sun.tools.attach.VirtualMachineDescriptor;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.jd.live.agent.bootstrap.LivePath.LIVE_JAR;

/**
 * The {@code AgentLoader} class provides functionality to attach a Java agent to a running JVM.
 * It parses command-line options, identifies the target JVM, and loads the agent into it.
 */
public class AgentLoader {

    private AgentLoader() {
    }

    /**
     * The main method to start the agent loader. It parses the command-line arguments,
     * finds the target JVM, and loads the specified agent into it.
     *
     * @param args the command-line arguments
     * @throws Exception if an  error occurs
     */
    public static void main(String[] args) throws Exception {
        AgentOption option = OptionParser.parse(args);
        if (option != null) {
            VirtualMachineDescriptor descriptor = getVmDescriptor(option);
            if (descriptor != null) {
                File path = getPath(option);
                if (path != null) {
                    option.setAgentPath(path.getAbsolutePath());
                    VirtualMachine machine = VirtualMachine.attach(descriptor);
                    VirtualMachine lvm = VirtualMachineFactory.getVirtualMachine(machine);
                    // Launch Agent
                    System.out.println("Start launching live agent");
                    System.out.println("Start attaching live agent to jvm, pid=" + descriptor.id());
                    lvm.loadAgent(new File(path, LIVE_JAR).getPath(), option.getAgentArgs());
                    lvm.detach();
                    System.out.println("Finish attaching live agent to jvm, pid=" + descriptor.id());
                }
            }
        }
    }

    /**
     * Retrieves the path to the agent directory. If the path is not provided or is invalid,
     * it prompts the user to enter a valid path interactively.
     *
     * @param option the agent options
     * @return the valid agent directory path
     * @throws IOException if an I/O error occurs while reading input
     */
    private static File getPath(AgentOption option) throws IOException {
        String path = option.getAgentPath();
        File file = path == null || path.isEmpty() ? LivePath.getRootPath(System.getenv(), null) : new File(path);
        long counter = 0;
        while (file == null || !option.isValidPath(file)) {
            counter++;
            if (option.isInteractive()) {
                System.out.print(counter == 1
                        ? "Enter agent directory (the live.jar in this directory is used as the entry by default):"
                        : "Enter agent directory:");
                path = new BufferedReader(new InputStreamReader(System.in)).readLine();
                file = path == null || path.isEmpty() ? null : new File(path.trim());
            } else {
                System.out.println("The agent directory is invalid. path=" + file);
                return null;
            }
        }
        return file;
    }

    /**
     * Retrieves the descriptor of the target JVM. If the process ID is not provided or is invalid,
     * it prompts the user to select a valid JVM process interactively.
     *
     * @param option the agent options
     * @return the descriptor of the target JVM
     * @throws IOException if an I/O error occurs while reading input
     */
    private static VirtualMachineDescriptor getVmDescriptor(AgentOption option) throws IOException {
        String jvmId = AgentOption.getJvmId();
        Map<String, VirtualMachineDescriptor> descriptors = VirtualMachine.list().stream()
                .filter(v -> !v.id().equals(jvmId))
                .collect(Collectors.toMap(VirtualMachineDescriptor::id, v -> v));
        String name = option.getName();
        String pid = option.getPid();

        if ((pid == null || pid.isEmpty()) && (name != null && !name.isEmpty())) {
            List<VirtualMachineDescriptor> targets = new ArrayList<>();
            for (VirtualMachineDescriptor descriptor : descriptors.values()) {
                if (descriptor.displayName().contains(name)) {
                    targets.add(descriptor);
                }
            }
            if (targets.size() == 1) {
                pid = targets.get(0).id();
            }
        }

        if (!descriptors.isEmpty() && option.isInteractive()) {
            long counter = 0;
            while (pid == null || pid.isEmpty() || !descriptors.containsKey(pid)) {
                counter++;
                if (counter == 1) {
                    System.out.println("Select the java process id to be attached.");
                    for (VirtualMachineDescriptor vm : descriptors.values()) {
                        System.out.println(vm.id() + " " + vm.displayName());
                    }
                }
                System.out.print("Please enter the jvmId:");
                // Read the jvm id entered by the user
                pid = new BufferedReader(new InputStreamReader(System.in)).readLine();
                pid = pid == null || pid.isEmpty() ? null : pid.trim();
            }
        }
        VirtualMachineDescriptor descriptor = pid == null || pid.isEmpty()
                ? null : descriptors.get(pid);
        if (descriptor == null) {
            System.out.println("The java process is not found. jvmId=" + pid);
            return null;
        }

        return descriptor;
    }
}