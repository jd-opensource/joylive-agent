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
package com.jd.live.agent.core.extension.condition;

import lombok.Getter;

import java.lang.annotation.Annotation;
import java.util.function.Function;

/**
 * ConditionContext is a utility class that holds context information for a conditional check.
 * It is typically used in frameworks or libraries that need to evaluate conditions based on class types,
 * annotations, and other related metadata.
 *
 * <p>This class provides methods to retrieve the type, annotation, and class loader associated with the context.
 * Additionally, it offers a method to retrieve configuration values based on a provided key, which can be used
 * to further customize conditional logic.</p>
 *
 * @author Zhiguo.Chen
 * @since 1.0.0
 */
public class ConditionContext {

    /**
     * The class type associated with this condition context.
     */
    @Getter
    private final Class<?> type;

    /**
     * The annotation associated with this condition context.
     */
    @Getter
    private final Annotation annotation;

    /**
     * The class loader associated with this condition context.
     */
    @Getter
    private final ClassLoader classLoader;

    /**
     * A function to retrieve optional configuration values.
     */
    private final Function<String, String> optional;

    private final Function<Class<?>, Condition> condition;

    /**
     * Constructs a new ConditionContext with the provided type, annotation, class loader, and optional configuration.
     *
     * @param type        The class type for this context.
     * @param annotation  The annotation for this context.
     * @param classLoader The class loader for this context.
     * @param optional    A function to retrieve optional configuration values.
     * @param condition   A function to retrieve a condition based on a class.
     */
    public ConditionContext(Class<?> type,
                            Annotation annotation,
                            ClassLoader classLoader,
                            Function<String, String> optional,
                            Function<Class<?>, Condition> condition) {
        this.type = type;
        this.annotation = annotation;
        this.classLoader = classLoader;
        this.optional = optional;
        this.condition = condition;
    }

    /**
     * Retrieves a configuration value based on the provided key. If the optional function is not set or the key is not
     * found, this method returns null.
     *
     * @param key The key to look up in the configuration.
     * @return The configuration value or null if not found.
     */
    public String geConfig(String key) {
        return optional == null ? null : optional.apply(key);
    }

    public Condition getCondition(Class<?> type) {
        return condition.apply(type);
    }

    /**
     * Creates a new ConditionContext based on the current context but with a different annotation.
     *
     * @param annotation The new annotation to use for the new context.
     * @return A new ConditionContext with the updated annotation.
     */
    public ConditionContext create(Annotation annotation) {
        return new ConditionContext(type, annotation, classLoader, optional, condition);
    }

}
