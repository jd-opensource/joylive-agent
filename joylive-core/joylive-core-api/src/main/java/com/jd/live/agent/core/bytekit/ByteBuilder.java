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

import com.jd.live.agent.core.bytekit.transformer.Resetter;
import com.jd.live.agent.core.plugin.definition.PluginDeclare;

import java.lang.instrument.Instrumentation;

/**
 * Provides a builder pattern interface for constructing byte manipulation operations.
 * This interface is designed to facilitate the dynamic modification or instrumentation
 * of byte code in runtime environments. It allows for the installation of instrumentation
 * logic and the appending of plugin declarations that can modify or enhance class definitions.
 *
 * @author Zhiguo.Chen
 * @since 1.0.0
 */
public interface ByteBuilder {

    /**
     * Installs instrumentation logic into the builder.
     * This method allows for the specification of custom instrumentation operations
     * that can be applied to classes at runtime. Instrumentation is a powerful tool
     * for modifying the behavior of existing classes or generating new ones dynamically.
     *
     * @param instrumentation The {@link Instrumentation} instance that provides methods
     *                        for instrumenting Java programming language code.
     * @return A {@link Resetter} instance that can be used to revert the instrumentation
     * changes made by this builder, if necessary.
     */
    Resetter install(Instrumentation instrumentation);

    /**
     * Appends a plugin declaration to the builder.
     * This method allows for the extension of the builder's capabilities by adding
     * plugins that can perform specific byte code manipulation tasks. Each plugin
     * can declare its own logic for modifying or enhancing classes.
     *
     * @param plugin The {@link PluginDeclare} instance representing the plugin to be added.
     * @return The current instance of {@link ByteBuilder}, allowing for method chaining.
     */
    ByteBuilder append(PluginDeclare plugin);
}
