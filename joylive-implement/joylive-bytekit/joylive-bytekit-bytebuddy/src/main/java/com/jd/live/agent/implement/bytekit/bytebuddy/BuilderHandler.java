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
package com.jd.live.agent.implement.bytekit.bytebuddy;

import com.jd.live.agent.core.extension.annotation.Extensible;
import net.bytebuddy.agent.builder.AgentBuilder;

import java.lang.instrument.Instrumentation;

/**
 * Defines a handler responsible for configuring the {@link AgentBuilder}, which is part of the Byte Buddy library.
 * This interface is marked as a functional interface, indicating that it is intended to be implemented by a single
 * abstract method (SAM) instance, such as a lambda expression or method reference.
 *
 * <p>The {@link BuilderHandler} is an extension point in the instrumentation process, allowing for custom
 * configurations of the {@link AgentBuilder}. This is crucial for defining how classes are intercepted and
 * modified at runtime. Implementations of this interface can specify class loaders, modules, and
 * specific classes to instrument, among other configurations.</p>
 *
 * <p>This interface is annotated with {@code @Extensible}, suggesting that it supports being extended
 * or implemented through the SkyWalking agent's plugin mechanism. This allows for dynamic addition of
 * instrumentation logic without modifying the core agent code.</p>
 *
 * @since 1.0.0 Initial version of the interface.
 */
@Extensible("BuilderHandler")
@FunctionalInterface
public interface BuilderHandler {

    int ORDER_RETRANSFORM_HANDLER = 0;

    int ORDER_IGNORED_HANDLER = ORDER_RETRANSFORM_HANDLER + 1;

    int ORDER_LOGGER_HANDLER = ORDER_IGNORED_HANDLER + 1;

    int ORDER_EXPORTER_HANDLER = ORDER_LOGGER_HANDLER + 1;

    /**
     * Configures the provided {@link AgentBuilder} with custom settings for instrumentation.
     *
     * <p>This method is called during the agent setup phase, allowing for the dynamic
     * instrumentation of Java classes. The configurations applied here dictate how and
     * which classes are modified at runtime. This is where you define the criteria for
     * class loading, method interception, and other bytecode enhancements.</p>
     *
     * @param builder         The {@link AgentBuilder} instance to be configured. This builder
     *                        is used to define the instrumentation rules and configurations.
     * @param instrumentation The {@link Instrumentation} instance provided by the JVM,
     *                        which allows for direct manipulation of classes at runtime.
     * @return The configured {@link AgentBuilder} instance, ready for use in class
     * instrumentation. It's important that this method returns a modified
     * version of the builder, as it is used in the subsequent instrumentation
     * process.
     */
    AgentBuilder configure(AgentBuilder builder, Instrumentation instrumentation);
}
