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
package com.jd.live.agent.core.parser.annotation;

import java.lang.annotation.*;

/**
 * Specifies one or more alternative names for a field when it is being deserialized from JSON.
 * This can be useful for supporting multiple JSON property names for a single Java field,
 * allowing for greater flexibility in handling JSON data with different naming conventions.
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface JsonAlias {
    /**
     * Defines one or more alternative names for the annotated field. These names are
     * used during deserialization to map JSON properties to the Java field, in addition
     * to the field's actual name.
     *
     * @return An array of alternative names.
     */
    String[] value() default {};
}
