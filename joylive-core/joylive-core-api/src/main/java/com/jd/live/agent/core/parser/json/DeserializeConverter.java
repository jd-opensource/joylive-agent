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
 * Specifies a custom converter that is used to deserialize JSON data into the annotated field.
 * This annotation allows for the customization of the deserialization process, enabling the use
 * of custom logic to convert JSON data into the desired field type.
 */
@Target(ElementType.FIELD) // Applies this annotation to fields only
@Retention(RetentionPolicy.RUNTIME) // Makes this annotation available at runtime
@Documented // Include this annotation in Javadoc
public @interface DeserializeConverter {
    /**
     * Specifies the class of the converter to be used for deserialization.
     * The specified class must implement the {@link JsonConverter} interface, providing
     * custom logic to convert from a JSON representation to the desired field type.
     *
     * @return The class of the converter to be used for deserialization.
     */
    Class<? extends JsonConverter<?, ?>> value();
}