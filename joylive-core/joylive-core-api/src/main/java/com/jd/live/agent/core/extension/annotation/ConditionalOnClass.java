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
 * An annotation that indicates a condition based on the presence of a specific class. If the
 * class specified by the {@code value} is present and can be loaded, then the condition is
 * considered to be met. This annotation is often used in configurations where the availability
 * of a class can enable or disable certain functionality or beans.
 * <p>
 * The annotation is marked with {@code @Conditional(dependOnLoader = true)}, indicating that
 * the condition depends on the class loader's ability to load the specified class.
 * <p>
 * This annotation can be repeated using {@code @ConditionalOnClasses} to define multiple class
 * presence conditions on the same element.
 *
 * @since 1.0.0
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(ConditionalOnClasses.class)
@Documented
@Conditional(dependOnLoader = true)
public @interface ConditionalOnClass {

    /**
     * Specifies the fully qualified name of the class that must be present for the condition
     * to be met. If the specified class can be loaded, then the annotated element is considered
     * eligible for whatever operation or processing is being conditionally controlled.
     *
     * @return The fully qualified name of the class that must be present.
     */
    String value();

}

