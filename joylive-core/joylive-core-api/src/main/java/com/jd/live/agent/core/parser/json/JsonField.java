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
package com.jd.live.agent.core.parser.json;

import java.lang.annotation.*;

/**
 * Specifies the name of a field in JSON. This annotation can be used to map a Java field to a differently
 * named field in JSON, facilitating serialization and deserialization when field names do not match between
 * the Java object model and the JSON data structure.
 *
 * <p>Applying this annotation to a field allows for customization of the field's name in JSON, providing
 * flexibility in handling JSON data with varying naming conventions.</p>
 *
 * @since 1.0.0
 */
@Target(ElementType.FIELD) // Indicates that this annotation is applicable to fields only.
@Retention(RetentionPolicy.RUNTIME) // Specifies that this annotation will be available at runtime for reflection.
@Documented // Indicates that this annotation should be documented by Javadoc and similar tools.
public @interface JsonField {
    /**
     * Defines the name of the field in JSON. If not provided, the Java field's name is used by default.
     *
     * @return The name of the field in JSON.
     */
    String value() default "";
}