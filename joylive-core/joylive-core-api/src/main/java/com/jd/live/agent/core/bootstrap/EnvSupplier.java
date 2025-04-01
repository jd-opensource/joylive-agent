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
package com.jd.live.agent.core.bootstrap;

import com.jd.live.agent.core.extension.annotation.Extensible;

import java.util.Map;

/**
 * Represents a functional interface that supplies an environment processing capability.
 * This interface is designed to be extensible, allowing implementations to define how
 * they wish to process or modify a given environment represented as a map.
 *
 * <p>This interface is marked as a functional interface, indicating that it is intended
 * to be implemented by a single abstract method. This allows instances of implementations
 * to be provided using lambda expressions, method references, or anonymous classes.</p>
 */
@FunctionalInterface
@Extensible
public interface EnvSupplier {

    int ORDER_SPRING_ENV_SUPPLIER = 0;

    int ORDER_NODE_ENV_SUPPLIER = ORDER_SPRING_ENV_SUPPLIER + 10;

    int ORDER_CONFIG_ENV_SUPPLIER = ORDER_NODE_ENV_SUPPLIER + 10;

    /**
     * Processes the given environment.
     * Implementations of this method are expected to perform operations on or with the provided
     * environment map. Such operations could include modifying the map's contents, performing
     * validations, or using the environment for configuration purposes.
     *
     * @param env A map representing the environment to be processed. The keys are environment
     *            variable names or identifiers, and the values are the corresponding values
     *            or settings associated with those keys.
     */
    void process(Map<String, Object> env);
}