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
package com.jd.live.agent.core.inject.annotation;

import com.jd.live.agent.bootstrap.classloader.ResourcerType;

import java.lang.annotation.*;

/**
 * Annotation for auto-injecting values into fields. It is used to specify that a field should
 * be injected with a value at runtime, supporting configuration-based injection. The annotation
 * can also indicate whether the injected value can be null.
 *
 * @author Zhiguo.Chen
 * @since 1.0.0
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Inject {

    /**
     * Specifies the name of the configuration field to be injected. The default is an empty string,
     * which typically implies that the field name itself will be used for injection.
     *
     * @return The configuration field name for injection.
     */
    String value() default "";

    /**
     * Indicates whether the field can be injected with a null value. The default is false,
     * meaning that null values are not allowed by default.
     *
     * @return {@code true} if the field can be injected with a null value; {@code false} otherwise.
     */
    boolean nullable() default false;

    /**
     * Indicates whether the field is a component. The default is false,
     *
     * @return {@code true} if the field is a component; {@code false} otherwise.
     */
    boolean component() default false;

    /**
     * Specifies the type of resource or class implementation to be loaded for the annotated field.
     * The default is set to {@code ResourcerType.CORE_IMPL}, indicating a core implementation resource
     * type will be loaded.
     *
     * @return The resource type to be loaded for the field.
     */
    ResourcerType loader() default ResourcerType.CORE_IMPL;

}

