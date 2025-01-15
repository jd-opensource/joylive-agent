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

import lombok.Getter;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static com.jd.live.agent.core.util.type.TypeScanner.scanner;

/**
 * A utility class for storing and retrieving information about fields within a Java class.
 *
 * <p>This class provides mechanisms to obtain metadata about class fields, including their types
 * and annotations, if any. It is designed to work with complex objects, ignoring primitive types,
 * arrays, and interfaces.</p>
 */
public class FieldList {
    /**
     * The class type for which field metadata is being stored.
     */
    protected Class<?> type;
    /**
     * A list of {@link FieldDesc} objects representing the metadata of each field within the class.
     */
    @Getter
    protected List<FieldDesc> fields = new LinkedList<>();
    /**
     * A map of field names to their corresponding {@link FieldDesc} objects for quick lookup.
     */
    @Getter
    protected Map<String, FieldDesc> fieldNames;

    /**
     * Constructor that initializes an instance of FieldList.
     *
     * @param type     The {@link Class} object of the class to analyze.
     * @param supplier A {@link FieldSupplier} functional interface that supplies additional metadata or configuration for each field.
     */
    public FieldList(Class<?> type, FieldSupplier supplier) {
        this.type = type;
        if (!type.isPrimitive() && !type.isArray() && !type.isInterface()) {
            scanner(type).scan(cls -> {
                for (Field field : cls.getDeclaredFields()) {
                    fields.add(new FieldDesc(type, field, supplier));
                }
            });
            fieldNames = new HashMap<>(fields.size());
            for (FieldDesc field : fields) {
                fieldNames.putIfAbsent(field.getField().getName(), field);
            }
        } else {
            fieldNames = new HashMap<>();
        }
    }

    public int size() {
        return fields.size();
    }

    public boolean isEmpty() {
        return fields.isEmpty();
    }

    /**
     * Retrieves the {@link FieldDesc} object representing metadata for a specific field by name.
     *
     * @param name The name of the field.
     * @return The {@link FieldDesc} object for the specified field, or {@code null} if not found.
     */
    public FieldDesc getField(final String name) {
        return name == null ? null : fieldNames.get(name);
    }

    /**
     * Iterates over all the field descriptions and applies the given consumer to each one.
     *
     * @param consumer The consumer to be applied to each field description.
     */
    public void forEach(final Consumer<FieldDesc> consumer) {
        if (consumer != null) {
            fields.forEach(consumer);
        }
    }
}
