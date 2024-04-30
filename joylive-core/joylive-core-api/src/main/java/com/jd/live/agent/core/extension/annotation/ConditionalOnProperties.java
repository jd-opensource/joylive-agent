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
 * An annotation that serves as a container for multiple {@code @ConditionalOnProperty} annotations. It allows
 * specifying multiple property-based conditions on a single element. For the annotated element to be considered,
 * all of the property conditions contained within this annotation must be satisfied.
 * <p>
 * This annotation inherits the conditional behavior from the {@code @Conditional} annotation, which means that the
 * evaluation process of whether the conditions are met is determined by the logic provided by the {@code @Conditional}
 * annotation's implementation.
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Conditional
public @interface ConditionalOnProperties {

    /**
     * An array of {@code @ConditionalOnProperty} annotations that define the property-based conditions that must be met.
     * Each {@code @ConditionalOnProperty} specifies a single property condition that must be true for the annotated element
     * to be considered eligible for whatever operation or processing is being conditionally controlled.
     *
     * @return An array of {@code @ConditionalOnProperty} annotations specifying the property conditions.
     */
    ConditionalOnProperty[] value();

}

