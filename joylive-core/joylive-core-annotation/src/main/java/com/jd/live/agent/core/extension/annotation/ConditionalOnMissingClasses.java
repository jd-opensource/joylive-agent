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
 * An annotation used to group multiple {@code @ConditionalOnMissingClass} annotations on the same
 * element. Since {@code @ConditionalOnMissingClass} is designed to check for the absence of specific
 * classes, this annotation allows for specifying multiple such conditions that must all be met for the
 * annotated element to be considered. It acts as a container that holds an array of {@code @ConditionalOnMissingClass}
 * instances, enabling the specification of multiple class absence conditions on the same element.
 * <p>
 * This annotation is also marked with {@code @Conditional(dependOnLoader = true)}, indicating that
 * the conditions it represents depend on the class loader's ability to not find the specified classes.
 *
 * @since 1.0.0
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Conditional(dependOnLoader = true)
public @interface ConditionalOnMissingClasses {

    /**
     * An array of {@code @ConditionalOnMissingClass} annotations representing the multiple class absence
     * conditions that must be met. Each condition specifies a class that must not be present for the
     * annotated element to be eligible for whatever operation or processing is being conditionally controlled.
     *
     * @return An array of {@code @ConditionalOnMissingClass} annotations specifying class absence conditions.
     */
    ConditionalOnMissingClass[] value();

}

