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
 * Specifies a custom converter class that should be used to serialize a field to JSON.
 * This annotation can be used to assign a specific {@link JsonConverter} to a field, enabling
 * custom serialization logic that may be required for complex field types or special formatting needs.
 *
 * <p>When this annotation is present on a field, the specified {@link JsonConverter} will be invoked
 * to convert the field's value to its JSON representation during serialization.</p>
 */
@Target(ElementType.FIELD) // Indicates that this annotation is applicable to fields only.
@Retention(RetentionPolicy.RUNTIME) // Specifies that this annotation will be available at runtime for reflection.
@Documented // Indicates that this annotation should be documented by Javadoc and similar tools.
public @interface SerializeConverter {

    /**
     * The {@link JsonConverter} class to be used for serializing the annotated field.
     *
     * @return The class of the converter to be used for serialization.
     */
    Class<JsonConverter<?, ?>> value();

}