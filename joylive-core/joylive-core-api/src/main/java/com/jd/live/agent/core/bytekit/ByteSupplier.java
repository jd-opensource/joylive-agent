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
package com.jd.live.agent.core.bytekit;

import com.jd.live.agent.core.extension.annotation.Extensible;

import java.lang.instrument.Instrumentation;
import java.util.Map;
import java.util.Set;

/**
 * Defines a contract for supplying {@link ByteBuilder} instances. This interface is aimed
 * at providing a way to create new instances of {@link ByteBuilder}, which can be used for
 * byte manipulation tasks such as bytecode enhancement or instrumentation. It is marked
 * as extensible, indicating it can be implemented in various ways to provide custom
 * byte builder implementations.
 *
 * @author Zhiguo.Chen
 * @since 1.0.0
 */
@Extensible("byteSupplier")
public interface ByteSupplier {

    /**
     * Creates a new instance of {@link ByteBuilder}.
     * This method is responsible for producing a fresh instance of {@link ByteBuilder},
     * which can then be used for constructing or modifying bytecode dynamically. Implementations
     * of this method should ensure that the returned {@link ByteBuilder} instance is properly
     * initialized and ready for use.
     *
     * @return A new instance of {@link ByteBuilder} ready for byte manipulation tasks.
     */
    ByteBuilder create();

    /**
     * Exports packages from source modules to target modules.
     *
     * @param instrumentation the instrumentation object
     * @param targets         a map of target modules and their corresponding source types
     * @param loaders         the class loaders
     */
    void export(Instrumentation instrumentation, Map<String, Set<String>> targets, ClassLoader... loaders);

    /**
     * Exports the specified package from the source module to the target module.
     *
     * @param instrumentation the instrumentation object used to modify the modules
     * @param sourceType      the class type in the source module (e.g., "java.util.Map")
     * @param sourcePackage   the package to export
     * @param targetType      the class type in the target module (e.g., "com.jd.live.agent.bootstrap.bytekit.context.MethodContext")
     * @param loaders         the class loaders to use when resolving the modules
     */
    void export(Instrumentation instrumentation, String sourceType, String sourcePackage, String targetType, ClassLoader... loaders);
}