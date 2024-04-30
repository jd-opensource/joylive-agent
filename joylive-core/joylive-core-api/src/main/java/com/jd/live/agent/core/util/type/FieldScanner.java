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
package com.jd.live.agent.core.util.type;

import java.lang.reflect.Field;
import java.util.function.Consumer;

/**
 * Scans the fields of a specified class, allowing custom operations to be performed on each field.
 * This can be used for reflective field analysis, modification, or annotation processing.
 *
 * @since 2024-01-20
 */
public class FieldScanner {

    private final Class<?> type;
    private final boolean declared;

    /**
     * Constructs a FieldScanner for the specified class.
     *
     * @param type     The class whose fields will be scanned.
     * @param declared If true, only declared fields of the class are considered;
     *                 if false, all fields up the class hierarchy are considered.
     */
    public FieldScanner(Class<?> type, boolean declared) {
        this.type = type;
        this.declared = declared;
    }

    /**
     * Performs the scanning of fields, applying the given consumer to each field found.
     *
     * @param consumer A consumer that performs operations on each field.
     */
    public void scan(Consumer<Field> consumer) {
        if (type == null || consumer == null)
            return;
        // The actual scanning logic, potentially recursive if `declared` is false.
        TypeScanner.build(type, declared ? t -> t.equals(type) : null).scan(type -> scan(type, consumer));
    }

    /**
     * Helper method to scan fields of a specific class and apply the consumer.
     *
     * @param type     The class whose fields are being scanned.
     * @param consumer The consumer to apply to each field.
     */
    protected void scan(Class<?> type, Consumer<Field> consumer) {
        Field[] fields = declared ? type.getDeclaredFields() : type.getFields();
        for (Field field : fields) {
            consumer.accept(field);
        }
    }
}
