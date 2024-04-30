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
package com.jd.live.agent.core.extension.annotation;

import java.lang.annotation.*;

/**
 * An annotation used to group multiple {@code @ConditionalOnClass} annotations on the same
 * element. Since {@code @ConditionalOnClass} is repeatable, this annotation acts as a container
 * that holds an array of {@code @ConditionalOnClass} instances. It allows for the specification
 * of multiple class-based conditions that must be met for the annotated element to be considered.
 * <p>
 * The presence of all specified classes, as determined by the ability of the class loader to
 * load them, is required for the condition to be satisfied.
 * <p>
 * This annotation is also marked with {@code @Conditional(dependOnLoader = true)}, signifying
 * that the conditions it represents depend on the class loader.
 *
 * @since 2024-01-21
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Conditional(dependOnLoader = true)
public @interface ConditionalOnClasses {

    /**
     * An array of {@code @ConditionalOnClass} annotations representing the multiple class-based
     * conditions that must be met.
     *
     * @return An array of {@code @ConditionalOnClass} annotations specifying class presence
     * conditions.
     */
    ConditionalOnClass[] value();

}

