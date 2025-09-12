/*
 * Copyright © ${year} ${owner} (${email})
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jd.live.agent.core.bytekit.type;

/**
 * A TypePool is responsible for creating type descriptions from class names.
 *
 * The key contract of this interface is that its implementations MUST NOT trigger
 * class loading (e.g., via Class.forName or ClassLoader.loadClass). Instead, they
 * should work by locating and parsing .class files from their bytecode.
 *
 * This is critical for safely inspecting type hierarchies from within a
 * {@code java.lang.instrument.ClassFileTransformer} without causing deadlocks.
 *
 * @since 1.0.0
 */
public interface TypePool {

    /**
     * Describes the given type by its name.
     *
     * @param name The fully qualified name of the type to describe.
     * @return A {@code TypeDesc} for the given type, or {@code null} if the
     * type could not be located or parsed from its bytecode.
     */
    TypeDesc describe(String name);

}