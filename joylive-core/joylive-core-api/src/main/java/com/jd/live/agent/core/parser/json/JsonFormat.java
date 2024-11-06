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
 * Specifies the format to be used for formatting/parsing JSON date/time values.
 * This annotation can be applied to fields of a date/time type to indicate how those fields
 * should be serialized into JSON and deserialized back into Java objects, allowing for
 * control over the date/time formats and timezones.
 *
 * <p>Using this annotation, developers can ensure consistent formatting of date/time fields
 * across different systems and locales, which is especially useful in internationalized applications.</p>
 *
 * @since 1.0.0
 */
@Target(ElementType.FIELD) // Indicates that this annotation is applicable to fields only.
@Retention(RetentionPolicy.RUNTIME) // Specifies that this annotation will be available at runtime for reflection.
@Documented // Indicates that this annotation should be documented by Javadoc and similar tools.
public @interface JsonFormat {

    /**
     * Defines the pattern used for date/time formatting and parsing.
     * The pattern should follow the conventions used by {@link java.text.SimpleDateFormat}.
     *
     * @return The pattern string.
     */
    String pattern() default "";

}
