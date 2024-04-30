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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * An annotation that indicates extensibility. When a class, interface, or enum is annotated with this,
 * it signifies that it is designed to be extensible, or in specific contexts, its implementations or subclasses
 * can be extended by users. The {@code value} attribute is used to specify the name of the extensibility,
 * which can be useful for documentation purposes or for retrieving more information about the nature of the
 * extensibility at runtime through reflection.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Extensible {

    /**
     * Specifies the name of the extensibility. This name can be used to identify or categorize different types
     * or purposes of extensions. If not specified, the default value is an empty string.
     *
     * @return The name of the extensibility.
     */
    String value() default "";
}

