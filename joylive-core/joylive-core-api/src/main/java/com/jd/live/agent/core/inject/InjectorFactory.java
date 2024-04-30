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
package com.jd.live.agent.core.inject;

import com.jd.live.agent.core.extension.ExtensionManager;
import com.jd.live.agent.core.extension.annotation.Extensible;
import com.jd.live.agent.core.util.option.Option;

/**
 * Defines the factory interface for creating {@link Injection} instances. This factory interface
 * supports creating injections with various configurations, including different environments,
 * class loaders, and embedding options. This flexibility allows for tailored injection mechanisms
 * suitable for specific scenarios.
 * <p>
 * The factory is marked as extensible to indicate that implementations can provide additional
 * functionalities or customizations beyond the standard injection creation methods.
 */
@Extensible("InjectorFactory")
public interface InjectorFactory {

    /**
     * Creates an {@link Injection} instance using the provided {@link ExtensionManager}.
     *
     * @param extensionManager The extension manager to be used in the injection process.
     * @return An {@link Injection} instance.
     */
    Injection create(ExtensionManager extensionManager);

    /**
     * Creates an {@link Injection} instance using the provided {@link ExtensionManager} and
     * {@link ClassLoader}.
     *
     * @param extensionManager The extension manager to be used in the injection process.
     * @param classLoader      The class loader to be used for loading classes during injection.
     * @return An {@link Injection} instance.
     */
    Injection create(ExtensionManager extensionManager, ClassLoader classLoader);

    /**
     * Creates an {@link Injection} instance using the provided {@link ExtensionManager} and
     * environment options.
     *
     * @param extensionManager The extension manager to be used in the injection process.
     * @param environment      The environment options to be considered during injection.
     * @return An {@link Injection} instance.
     */
    Injection create(ExtensionManager extensionManager, Option environment);

    /**
     * Creates an {@link Injection} instance using the provided {@link ExtensionManager},
     * environment options, and {@link ClassLoader}.
     *
     * @param extensionManager The extension manager to be used in the injection process.
     * @param environment      The environment options to be considered during injection.
     * @param classLoader      The class loader to be used for loading classes during injection.
     * @return An {@link Injection} instance.
     */
    Injection create(ExtensionManager extensionManager, Option environment, ClassLoader classLoader);

    /**
     * Creates an {@link Injection} instance using the provided {@link ExtensionManager},
     * environment options, {@link ClassLoader}, and embedding option.
     *
     * @param extensionManager The extension manager to be used in the injection process.
     * @param environment      The environment options to be considered during injection.
     * @param classLoader      The class loader to be used for loading classes during injection.
     * @param embed            Indicates whether the injection should be embedded.
     * @return An {@link Injection} instance.
     */
    Injection create(ExtensionManager extensionManager, Option environment, ClassLoader classLoader, boolean embed);

}

