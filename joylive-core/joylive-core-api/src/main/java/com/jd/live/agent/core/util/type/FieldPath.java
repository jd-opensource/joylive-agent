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
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Represents a path to a field within an object, allowing for nested field access.
 * Implements the {@link ObjectGetter} interface to provide a way to retrieve the value
 * at the specified field path from an object.
 */
public class FieldPath implements ObjectGetter {

    protected final String path;

    protected Field field;

    protected List<Field> fields;

    /**
     * Constructs a new FieldPath instance for the specified type and path.
     *
     * @param type the class type containing the fields
     * @param path the path to the field (e.g., "field1.field2")
     */
    public FieldPath(Class<?> type, String path) {
        this.path = path;
        this.fields = parse(type, path);
        this.field = fields != null && fields.size() == 1 ? fields.get(0) : null;
    }

    /**
     * Constructs a new {@link FieldPath} instance for the specified type and path.
     * This constructor attempts to load the class specified by the type string and
     * parse the field path to create a list of fields. If the class cannot be found,
     * the fields list will be null, and the single field reference will also be null.
     *
     * @param type the fully qualified name of the class containing the fields
     * @param path the path to the field (e.g., "field1.field2")
     */
    public FieldPath(String type, String path) {
        this.path = path;
        try {
            this.fields = parse(Thread.currentThread().getContextClassLoader().loadClass(type), path);
            this.field = fields != null && fields.size() == 1 ? fields.get(0) : null;
        } catch (ClassNotFoundException ignored) {
        }
    }

    @Override
    public Object get(Object target) {
        return get(target, null);
    }

    /**
     * Retrieves the value at the specified field path from the target object.
     * If the field path is not valid or an exception occurs, it uses the provided supplier
     * to provide a default value.
     *
     * @param target   the object from which to retrieve the value
     * @param supplier a supplier function that provides a default value if the field path is invalid or an exception occurs
     * @return the value at the specified field path, or the default value provided by the supplier if an error occurs
     */
    public Object get(Object target, Supplier<Object> supplier) {
        try {
            Object result = null;
            if (field != null) {
                result = field.get(target);
            } else if (fields != null) {
                for (Field field : fields) {
                    result = field.get(target);
                    target = result;
                }
            } else {
                result = supplier == null ? null : supplier.get();
            }
            return result;
        } catch (Throwable e) {
            return supplier == null ? null : supplier.get();
        }
    }

    /**
     * Parses the given path string into a list of Field objects.
     * Handles nested field paths separated by dots.
     *
     * @param type the class type containing the fields
     * @param path the path string to parse
     * @return a list of Field objects representing the parsed path
     */
    protected List<Field> parse(Class<?> type, String path) {
        if (path == null || path.isEmpty()) {
            return null;
        }
        List<Field> result = new ArrayList<>(2);
        Field field;
        int start = 0;
        int pos = path.indexOf('.', start);
        while (pos >= start) {
            field = getField(type, path.substring(start, pos));
            if (field == null) {
                return null;
            }
            type = field.getType();
            result.add(field);
            start = pos + 1;
            pos = path.indexOf('.', start);
        }
        if (start < path.length() - 1) {
            field = getField(type, path.substring(start));
            if (field == null) {
                return null;
            }
            result.add(field);
        }
        return result;

    }

    /**
     * Retrieves the Field object for the specified name from the given class type.
     *
     * @param type the class type containing the field
     * @param name the name of the field
     * @return the Field object
     */
    private Field getField(Class<?> type, String name) {
        if (name.isEmpty()) {
            return null;
        }
        Field field;
        while (type != null && type != Object.class) {
            try {
                field = type.getDeclaredField(name);
                field.setAccessible(true);
                return field;
            } catch (NoSuchFieldException e) {
                type = type.getSuperclass();
            }
        }
        return null;
    }
}
