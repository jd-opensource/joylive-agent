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
 * Indicates that the annotated type is conditional and should be considered for some operations
 * only if certain conditions are met. The conditions are specified by a matcher class whose
 * name is provided as the value of the annotation. This annotation can be used to mark classes
 * that should only be loaded, instantiated, or processed when the specified conditions are true.
 *
 * @since 1.0.0
 */
@Target({ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Conditional {

    /**
     * Specifies the condition matcher class name that determines whether the conditions
     * for applying the annotated type are met.
     *
     * @return The fully qualified name of the condition matcher class.
     */
    String value() default "";

    /**
     * Indicates whether the condition depends on the class loader. If {@code true}, the condition
     * will take into account the class loader of the annotated type. This can be important when
     * class loading behavior is part of the condition.
     *
     * @return {@code true} if the condition depends on the class loader; {@code false} otherwise.
     */
    boolean dependOnLoader() default false;

}
