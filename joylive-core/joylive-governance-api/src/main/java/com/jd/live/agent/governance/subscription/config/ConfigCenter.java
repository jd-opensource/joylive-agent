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
package com.jd.live.agent.governance.subscription.config;

import com.jd.live.agent.governance.config.ConfigCenterConfig;

import java.util.function.Consumer;

/**
 * An interface for managing configurations.
 *
 * @since 1.0
 */
public interface ConfigCenter {

    String COMPONENT_CONFIG_CENTER = "configCenter";

    /**
     * Returns a Configurator instance for the specified configuration name.
     *
     * @param name The ConfigName object representing the namespace, name, and profile of the configuration.
     * @return A Configurator instance for the specified configuration.
     */
    Configurator getConfigurator(ConfigName name);

    /**
     * Returns a default Configurator instance.
     *
     * @return A default Configurator instance.
     */
    Configurator getConfigurator();

    ConfigCenterConfig getConfig();

    /**
     * Invokes the given consumer if a configurator is present.
     *
     * @param consumer The consumer to be invoked if a configurator is present.
     */
    default void ifPresent(Consumer<Configurator> consumer) {
        Configurator configurator = getConfigurator();
        if (configurator != null && consumer != null) {
            consumer.accept(configurator);
        }
    }

}


