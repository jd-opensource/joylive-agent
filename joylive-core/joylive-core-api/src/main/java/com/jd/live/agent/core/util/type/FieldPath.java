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

import com.jd.live.agent.bootstrap.util.type.FieldAccessor;
import com.jd.live.agent.bootstrap.util.type.ObjectGetter;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import static com.jd.live.agent.bootstrap.util.type.FieldAccessorFactory.getAccessor;

/**
 * Represents a path to a field within an object, allowing for nested field access.
 * Implements the {@link ObjectGetter} interface to provide a way to retrieve the value
 * at the specified field path from an object.
 */
public class FieldPath implements ObjectGetter {

    @Getter
    private final Class<?> type;

    @Getter
    private final String path;

    private final FieldAccessor field;

    private final List<FieldAccessor> fields;

    private final ObjectGetter getter;

    /**
     * Constructs a new FieldPath instance for the specified type and path.
     *
     * @param type the class type containing the fields
     * @param path the path to the field (e.g., "field1.field2")
     */
    public FieldPath(Class<?> type, String path) {
        this.type = type;
        this.path = path;
        this.fields = parse(type, path);
        this.field = fields != null && fields.size() == 1 ? fields.get(0) : null;
        this.getter = buildGetter();
    }

    @Override
    public Object get(Object target) {
        return get(target, (Supplier<Object>) null);
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
            Object result = getter.get(target);
            if (result == null && supplier != null) {
                result = supplier.get();
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
    private List<FieldAccessor> parse(Class<?> type, String path) {
        if (path == null || path.isEmpty()) {
            return null;
        }
        List<FieldAccessor> result = new ArrayList<>(2);
        int start = 0;
        int pos = path.indexOf('.', start);
        FieldAccessor accessor;
        while (pos >= start) {
            accessor = getAccessor(type, path.substring(start, pos));
            if (accessor == null) {
                return null;
            }
            result.add(accessor);
            type = accessor.getField().getType();
            start = pos + 1;
            pos = path.indexOf('.', start);
        }
        if (start < path.length() - 1) {
            accessor = getAccessor(type, path.substring(start));
            if (accessor == null) {
                return null;
            }
            result.add(accessor);
        }
        return result;
    }

    private ObjectGetter buildGetter() {
        if (field != null) {
            return field;
        } else if (fields != null && !fields.isEmpty()) {
            return o -> {
                Object result = null;
                for (FieldAccessor field : fields) {
                    result = field.get(o);
                    o = result;
                }
                return result;
            };
        } else {
            return o -> null;
        }
    }
}
