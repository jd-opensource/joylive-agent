package com.jd.live.agent.bootstrap;

import com.jd.live.agent.bootstrap.option.AgentOption;
import com.jd.live.agent.bootstrap.option.OptionParser;
import com.sun.tools.attach.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
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
     * @throws IOException                  if an I/O error occurs
     * @throws AttachNotSupportedException  if the target JVM does not support attaching
     * @throws AgentLoadException           if the agent cannot be loaded
     * @throws AgentInitializationException if the agent initialization fails
     */
    public static void main(String[] args) throws IOException, AttachNotSupportedException, AgentLoadException, AgentInitializationException {
        AgentOption option = OptionParser.parse(args);
        if (option != null) {
            VirtualMachineDescriptor descriptor = getVmDescriptor(option);
            if (descriptor != null) {
                File path = getPath(option);
                if (path != null) {
                    option.setAgentPath(path.getAbsolutePath());
                    VirtualMachine vm = VirtualMachine.attach(descriptor);
                    // Launch Agent
                    vm.loadAgent(new File(path, LIVE_JAR).getPath(), option.getAgentArgs());
                    vm.detach();
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
        String pid = AgentOption.getPid();
        Map<String, VirtualMachineDescriptor> descriptors = VirtualMachine.list().stream()
                .filter(v -> !v.id().equals(pid))
                .collect(Collectors.toMap(VirtualMachineDescriptor::id, v -> v));
        String jvmId = option.getProcessId();
        if (!descriptors.isEmpty() && option.isInteractive()) {
            long counter = 0;
            while (jvmId == null || jvmId.isEmpty() || !descriptors.containsKey(jvmId)) {
                counter++;
                if (counter == 1) {
                    System.out.println("Select the java process id to be attached.");
                    for (VirtualMachineDescriptor vm : descriptors.values()) {
                        System.out.println(vm.id() + " " + vm.displayName());
                    }
                }
                System.out.print("Please enter the pid:");
                // Read the jvm id entered by the user
                jvmId = new BufferedReader(new InputStreamReader(System.in)).readLine();
                jvmId = jvmId == null || jvmId.isEmpty() ? null : jvmId.trim();
            }
        }
        VirtualMachineDescriptor descriptor = jvmId == null || jvmId.isEmpty()
                ? null : descriptors.get(jvmId);
        if (descriptor == null) {
            System.out.println("The java process is not found. pid=" + jvmId);
            return null;
        }

        return descriptor;
    }
}