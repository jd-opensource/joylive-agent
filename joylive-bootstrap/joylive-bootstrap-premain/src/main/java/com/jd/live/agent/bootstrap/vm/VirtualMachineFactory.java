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
package com.jd.live.agent.bootstrap.vm;

import com.sun.tools.attach.VirtualMachine;
import sun.tools.attach.HotSpotVirtualMachine;

/**
 * The {@code VirtualMachineFactory} class provides a factory method to obtain a {@link VirtualMachine} instance.
 * If the provided {@link VirtualMachine} is an instance of {@link HotSpotVirtualMachine}, it wraps it in a {@link LiveVirtualMachine}.
 */
public class VirtualMachineFactory {

    private static final String HOT_SPOT_VIRTUAL_MACHINE = "sun.tools.attach.HotSpotVirtualMachine";

    /**
     * Returns a {@link VirtualMachine} instance. If the provided {@link VirtualMachine} is an instance of
     * {@link HotSpotVirtualMachine}, it wraps it in a {@link LiveVirtualMachine}. Otherwise, it returns the provided instance.
     *
     * @param machine the {@link VirtualMachine} instance to be checked and possibly wrapped
     * @return a {@link VirtualMachine} instance, either the provided one or a wrapped {@link LiveVirtualMachine}
     */
    public static VirtualMachine getVirtualMachine(VirtualMachine machine) {
        if (isInherited(machine, HOT_SPOT_VIRTUAL_MACHINE)) {
            return new LiveVirtualMachine(machine);
        }
        return machine;
    }

    private static boolean isInherited(VirtualMachine machine, String className) {
        if (machine == null || className == null || className.isEmpty()) {
            return false;
        }
        Class<?> type = machine.getClass();
        while (type != null && !type.equals(Object.class)) {
            if (className.equals(type.getName())) {
                return true;
            }
            type = type.getSuperclass();
        }
        return false;
    }
}
