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
 * Annotation to specify a custom classloader for a field. This allows for dynamic loading of resources
 * or classes for the annotated field, based on the specified resource type. It can be used to inject
 * different implementations or resources at runtime, enhancing the flexibility of the application's
 * resource management and class loading mechanism.
 *
 * @author Zhiguo.Chen
 * @since 1.0.0
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface InjectLoader {

    /**
     * Specifies the type of resource or class implementation to be loaded for the annotated field.
     * The default is set to {@code ResourcerType.CORE_IMPL}, indicating a core implementation resource
     * type will be loaded.
     *
     * @return The resource type to be loaded for the field.
     */
    ResourcerType value() default ResourcerType.CORE_IMPL;

}

