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
package com.jd.live.agent.bootstrap.util.type;

/**
 * Functional interface indicating method handle support capability.
 * Provides default implementations for different JVM versions.
 */
@FunctionalInterface
public interface MethodVersion {

    MethodVersion REFLECT = new NoneVersion();

    MethodVersion AUTO = new JVMVersion();

    /**
     * Checks if method handles are supported.
     *
     * @return true if running on JVM with method handle support (Java 7+)
     */
    boolean supportMethodHandle();

    /**
     * Implementation for environments without method handle support.
     */
    class NoneVersion implements MethodVersion {

        @Override
        public boolean supportMethodHandle() {
            return false;
        }
    }

    /**
     * Implementation that detects Java 7+ method handle support.
     * Uses runtime detection of {@code java.lang.invoke.MethodHandle}.
     */
    class JVMVersion implements MethodVersion {
        protected static boolean JAVA7_OR_HIGHER = detectJava7();

        private static boolean detectJava7() {
            try {
                Class.forName("java.lang.invoke.MethodHandle", true, ClassLoader.getSystemClassLoader());
                return true;
            } catch (ClassNotFoundException e) {
                return false;
            }
        }

        @Override
        public boolean supportMethodHandle() {
            return JAVA7_OR_HIGHER;
        }
    }
}
