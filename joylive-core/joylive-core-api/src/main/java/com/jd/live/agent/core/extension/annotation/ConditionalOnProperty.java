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
 * An annotation that indicates a condition based on a specific property. This condition is considered
 * met if the specified property matches the given value, or if the property is missing and {@code matchIfMissing}
 * is set to {@code true}. This annotation can be used to conditionally enable or disable certain functionality
 * based on the presence and value of specific properties in the environment.
 * <p>
 * This annotation is marked as {@code @Repeatable}, meaning it can be used multiple times on the same element
 * in conjunction with the {@code @ConditionalOnProperties} annotation, which acts as a container for multiple
 * {@code @ConditionalOnProperty} annotations.
 * <p>
 * The {@code @Conditional} annotation specifies that the conditionality is determined by the presence and value
 * of the specified property. The evaluation logic is provided by the implementation of the {@code @Conditional}
 * annotation.
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Repeatable(ConditionalOnProperties.class)
@Conditional
public @interface ConditionalOnProperty {
    /**
     * The value of the property to match. The condition is met if the property's value matches this value.
     * If no value is specified, the presence of the property name alone can be used to meet the condition,
     * depending on the implementation and other attributes.
     *
     * @return The expected value of the property.
     */
    String value() default "";

    /**
     * The name of the property to check. This is the key used to look up the property in the environment.
     *
     * @return The name of the property.
     */
    String[] name() default {};

    /**
     * Specifies whether the condition should be considered met if the property is missing. If {@code true},
     * the absence of the specified property will not prevent the condition from being met.
     *
     * @return {@code true} if the condition should be considered met when the property is missing; {@code false} otherwise.
     */
    boolean matchIfMissing() default false;

    /**
     * Defines the logical relation to be applied when evaluating multiple {@link ConditionalOnProperty} conditions.
     *
     * @return the {@link ConditionalRelation} that determines how multiple conditions are combined.
     */
    ConditionalRelation relation() default ConditionalRelation.OR;

    /**
     * Specifies whether the comparison between the actual property value and the {@link #value()}
     * should be case-sensitive.
     * <p>
     * By default, this attribute is set to {@code false}, meaning the comparison is case-insensitive.
     * Setting this to {@code true} requires an exact case match between the specified value and the
     * actual property value for the condition to be considered true.
     * </p>
     *
     * @return {@code true} if the comparison should be case-sensitive; {@code false} otherwise.
     */
    boolean caseSensitive() default false;

}

