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
package com.jd.live.agent.core.plugin.definition;

import com.jd.live.agent.core.bytekit.matcher.ElementMatcher;
import com.jd.live.agent.core.bytekit.type.TypeDesc;
import com.jd.live.agent.core.extension.annotation.Extensible;

/**
 * Defines a plugin's configuration including its matching criteria and interceptors.
 * This interface is intended to be implemented by plugins to specify how they should
 * be applied to target classes and which interceptors should be used.
 * <p>
 * The plugin system may use the {@link #getMatcher()} method to determine if a plugin
 * should be applied to a given class based on its characteristics.
 * <p>
 * The {@link #getInterceptors()} method provides the interceptor definitions that should
 * be used to modify or extend the behavior of the matched classes.
 * <p>
 * The order constants (e.g., {@link #ORDER_SYSTEM}, {@link #ORDER_APPLICATION}, etc.)
 * define the precedence of plugin application, allowing for a structured layering
 * of plugins based on their purpose and dependencies.
 * <p>
 * This interface is marked as {@code @Extensible("PluginDefinition")} to indicate
 * that it is intended for extension by plugin developers.
 *
 * @author Zhiguo.Chen
 * @since 2024-01-20
 */
@Extensible("PluginDefinition")
public interface PluginDefinition {

    /**
     * Order for system-level plugins. These are typically the first to be applied.
     */
    int ORDER_SYSTEM = 1000;

    /**
     * Order for application-level plugins. These are applied after system plugins.
     */
    int ORDER_APPLICATION = ORDER_SYSTEM + 1000;

    /**
     * Order for transmission-level plugins. These are applied after application plugins.
     */
    int ORDER_TRANSMISSION = ORDER_APPLICATION + 1000;

    /**
     * Order for registry-level plugins. These are applied after transmission plugins.
     */
    int ORDER_REGISTRY = ORDER_TRANSMISSION + 1000;

    /**
     * Order for router-level plugins. These are applied after registry plugins.
     */
    int ORDER_ROUTER = ORDER_REGISTRY + 1000;

    /**
     * Order for protect-level plugins. These are the last to be applied before user plugins.
     */
    int ORDER_PROTECT = ORDER_ROUTER + 1000;

    /**
     * Order for failover-level plugins. These are the last to be applied before user plugins.
     */
    int ORDER_FAILOVER = ORDER_PROTECT + 1000;

    /**
     * Returns the matcher used to determine if this plugin should be applied to a given class.
     *
     * @return The element matcher that defines the criteria for matching classes.
     */
    ElementMatcher<TypeDesc> getMatcher();

    /**
     * Returns an array of interceptor definitions that should be used to modify
     * or extend the behavior of the matched classes.
     *
     * @return An array of interceptor definitions for this plugin.
     */
    InterceptorDefinition[] getInterceptors();
}

