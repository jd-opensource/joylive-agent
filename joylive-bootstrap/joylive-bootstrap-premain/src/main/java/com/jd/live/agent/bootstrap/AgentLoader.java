package com.jd.live.agent.bootstrap;

import com.sun.tools.attach.AgentInitializationException;
import com.sun.tools.attach.AgentLoadException;
import com.sun.tools.attach.AttachNotSupportedException;
import com.sun.tools.attach.VirtualMachine;
import com.sun.tools.attach.VirtualMachineDescriptor;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;

public class AgentLoader {
    private AgentLoader() {
    }

    public static void main(String[] args)
            throws IOException, AttachNotSupportedException, AgentLoadException, AgentInitializationException {
        List<VirtualMachineDescriptor> vmDescriptors = VirtualMachine.list();

        if (vmDescriptors.isEmpty()) {
            System.out.println("No Java process found!");
            return;
        }

        System.out.println("Select the Java process that you want to use the agent.");
        for (int i = 0; i < vmDescriptors.size(); i++) {
            VirtualMachineDescriptor descriptor = vmDescriptors.get(i);
            System.out.println(i + ": " + descriptor.id() + " " + descriptor.displayName());
        }

        // Read the sequence number entered by the user
        BufferedReader userInputReader = new BufferedReader(new InputStreamReader(System.in));
        System.out.print("Please enter the Java program number to be used by the agent:");
        int selectedProcessIndex = Integer.parseInt(userInputReader.readLine());

        if (selectedProcessIndex < 0 || selectedProcessIndex >= vmDescriptors.size()) {
            System.out.println("Invalid program number!");
            return;
        }

        // Connect to the selected virtual machine
        VirtualMachineDescriptor selectedDescriptor = vmDescriptors.get(selectedProcessIndex);
        System.out.println("The process ID you selected is:" + selectedDescriptor.id());

        VirtualMachine vm = VirtualMachine.attach(selectedDescriptor);

        // Obtain the agent directory
        System.out.print("Enter the directory where the agent is located (the live.jar in this directory is used as the entry by default):");
        String agentPath = userInputReader.readLine();

        // Obtain the parameters of the incoming agent
        System.out.print("Please enter the parameters passed to the agent (can be empty, the default parameter is agentPath):");
        String agentArgs = "agentPath=" + agentPath + "," + userInputReader.readLine();
        userInputReader.close();

        try {
            // Launch Agent
            vm.loadAgent(agentPath + "/live.jar", agentArgs);
            vm.detach();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}