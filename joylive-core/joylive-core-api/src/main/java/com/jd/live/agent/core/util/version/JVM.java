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
package com.jd.live.agent.core.util.version;

import lombok.Getter;

/**
 * A singleton class that holds information about the current Java Virtual Machine (JVM)
 * and the operating system it is running on.
 */
@Getter
public class JVM {

    public static final String JAVA_VERSION = "java.version";
    public static final String JAVA_VM_VENDOR = "java.vm.vendor";
    public static final String USER_HOME = "user.home";
    public static final String OS_NAME = "os.name";
    public static final String OS_ARCH = "os.arch";
    private static final JVM INSTANCE = new JVM();

    private final Version version;

    private final String vendor;

    private final String home;

    private final String osName;

    private final String osArch;

    private final boolean mac;


    public JVM() {
        version = new Version(System.getProperty(JAVA_VERSION));
        vendor = System.getProperty(JAVA_VM_VENDOR);
        home = System.getProperty(USER_HOME);
        osName = System.getProperty(OS_NAME);
        osArch = System.getProperty(OS_ARCH);
        mac = osName.toLowerCase().contains("mac");
    }

    public static JVM instance() {
        return INSTANCE;
    }

}
