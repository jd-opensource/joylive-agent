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

import java.lang.reflect.Array;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

import static com.jd.live.agent.core.util.type.TypeScanner.UNTIL_OBJECT;

/**
 * A utility class that resolves the value of an object's property based on a given path expression.
 * It supports accessing nested properties and indexing into collections and arrays.
 */
public class ValuePath implements ObjectGetter {

    private static final Map<String, ValuePath> VALUE_PATHS = new ConcurrentHashMap<>();

    @Getter
    protected final String path;

    protected final List<PropertyPath> paths;

    private final Predicate<Object> predicate;

    /**
     * Constructs a ValuePath instance with the specified path string.
     *
     * @param path The path string indicating how to retrieve a value from the target object.
     */
    public ValuePath(String path) {
        this(path, null);
    }

    /**
     * Constructs a ValuePath instance with the specified path string and predicate.
     *
     * @param path      The path string indicating how to retrieve a value from the target object.
     * @param predicate A predicate to test the retrieved value. If the test fails, null is returned.
     */
    public ValuePath(String path, Predicate<Object> predicate) {
        this.path = path;
        this.predicate = predicate;
        this.paths = parse(path);
    }

    /**
     * Creates a new ValuePath instance with the specified path.
     *
     * @param path the path to the value
     * @return a new ValuePath instance
     */
    public static ValuePath of(String path) {
        return VALUE_PATHS.computeIfAbsent(path, ValuePath::new);
    }

    @SuppressWarnings("unchecked")
    @Override
    public Object get(Object target) {
        Object result = null;
        int index = 0;
        for (PropertyPath propertyPath : paths) {
            result = getObject(target, propertyPath);
            if (result == null) {
                result = index == 0 && paths.size() > 1 && target instanceof Map ? ((Map<String, Object>) target).get(path) : null;
                return result;
            } else if (index == paths.size() - 1) {
                return result;
            } else if (predicate != null && !predicate.test(result)) {
                return null;
            }
            target = result;
            index++;
        }
        return result;
    }

    /**
     * Parses the given path string into a list of PropertyPath objects.
     *
     * @param path The path string to parse.
     * @return A list of PropertyPath objects representing the parsed paths.
     */
    protected List<PropertyPath> parse(String path) {
        List<PropertyPath> result = new LinkedList<>();
        if (path != null) {
            int level = 1;
            int start = 0;
            int pos = path.indexOf('.', start);
            if (pos == -1) {
                result.add(new PropertyPath(path));
            } else {
                String key;
                while (pos >= start) {
                    key = path.substring(start, pos);
                    if (!key.isEmpty()) {
                        result.add(new PropertyPath(key));
                    }
                    start = pos + 1;
                    pos = path.indexOf('.', start);
                    level++;
                }
                if (level > 1 && start < path.length() - 1) {
                    result.add(new PropertyPath(path.substring(start)));
                }
            }
        }
        return result;
    }

    /**
     * Retrieves the value described by the specified PropertyPath from the target object.
     *
     * @param target The target object.
     * @param path   The property path to retrieve the value for.
     * @return The retrieved value or null if it cannot be obtained.
     */
    protected Object getObject(Object target, PropertyPath path) {
        Object result = target == null || path.isEmpty() ? null : getProperty(target, path.getField());
        if (result != null && path.isIndexed()) {
            if (result instanceof Map) {
                result = ((Map<?, ?>) result).get(path.getIndex());
            } else if (result instanceof List<?>) {
                result = getListItem((List<?>) result, path.getIndex());
            } else if (result.getClass().isArray()) {
                result = getArrayItem(result, path.getIndex());
            }
        }
        return result;
    }

    /**
     * Retrieves the value of a specified property from the target object.
     * This method first checks if the target object is of type Map, and if so,
     * it obtains the property value using key-value retrieval. If the target object
     * is not a Map, it attempts to retrieve the property value using reflection.
     * The reflection part utilizes a custom utility class, ClassUtils, to obtain
     * information about the target object's class and field descriptors, and then
     * accesses the property value through the field descriptor. If the target class
     * or field does not meet the required conditions, null is returned.
     *
     * @param target   The target object from which to retrieve the property value,
     *                 which can be a Map or any other Java object.
     * @param property The name of the property to retrieve.
     * @return The retrieved property value, or null if it cannot be obtained.
     */
    protected Object getProperty(Object target, String property) {
        if (target instanceof Map) {
            return ((Map<?, ?>) target).get(property);
        }
        Class<?> type = target.getClass();
        if (UNTIL_OBJECT.test(type)) {
            FieldDesc fieldDesc = ClassUtils.describe(type).getFieldList().getField(property);
            if (fieldDesc != null) {
                return fieldDesc.get(target);
            }
        }
        return null;
    }

    /**
     * Retrieves an item from an array at the specified index.
     * This method attempts to parse the index as an integer and then checks if it is within the bounds of the array.
     * If the index is valid, it returns the item at that index. If the index is invalid or if any exception occurs
     * during the parsing of the index (e.g., if the index is not a valid integer), the method returns null.
     *
     * @param result The array from which to retrieve an item. This object should be an array type.
     * @param index  The index of the item to retrieve, represented as a String.
     * @return The item at the specified index in the array, or null if the index is invalid or if an exception occurs.
     */
    protected Object getArrayItem(Object result, String index) {
        try {
            int idx = Integer.parseInt(index);
            int length = Array.getLength(result);
            return idx >= 0 && idx < length ? Array.get(result, idx) : null;
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    /**
     * Retrieves an item from a list at the specified index.
     * This method attempts to parse the index as an integer and then checks if it is within the bounds of the list.
     * If the index is valid, it returns the item at that index from the list. If the index is invalid or if any
     * exception occurs during the parsing of the index (e.g., if the index is not a valid integer), the method
     * returns null to indicate the failure to retrieve an item.
     *
     * @param target The list from which to retrieve an item. This should be an instance of List.
     * @param index  The index of the item to retrieve, represented as a String. This index will be parsed into an integer.
     * @return The item at the specified index in the list, or null if the index is invalid or if an exception occurs.
     */
    protected Object getListItem(List<?> target, String index) {
        try {
            int idx = Integer.parseInt(index);
            return idx >= 0 && idx < target.size() ? target.get(idx) : null;
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    /**
     * A helper class representing a property path, which may include a field name and an index.
     * The purpose of this class is to parse and store the field name and the optional index
     * when the property path is in a format that denotes indexing, such as "fieldName[index]".
     */
    @Getter
    protected static class PropertyPath {

        private String field; // The field name part of the property path
        private String index; // The index part of the property path (if any)

        /**
         * Constructs a PropertyPath object by parsing the provided field string.
         * If the field string ends with ']', it tries to extract the index part.
         * If there is no '[' or the '[' is at the start, it treats the whole string
         * as an index or a field name respectively.
         *
         * @param field The string representation of the property path, which may include an index.
         */
        public PropertyPath(String field) {
            int length = field.length();
            if (length > 0 && field.charAt(length - 1) == ']') {
                // Attempt to parse the indexed field format, e.g., "fieldName[2]"
                int pos = field.lastIndexOf('[');
                if (pos > 0) {
                    this.index = field.substring(pos + 1, length - 1);
                    this.field = field.substring(0, pos);
                } else if (pos == 0) {
                    // The whole string is treated as an index, e.g., "[2]"
                    this.field = field.substring(1, length - 1);
                    this.index = null;
                }
            } else {
                // No index present, the whole string is the field name
                this.field = field;
                this.index = null;
            }
        }

        /**
         * Checks if the field is empty.
         *
         * @return true if the field is null or empty, false otherwise.
         */
        public boolean isEmpty() {
            return field == null || field.isEmpty();
        }

        /**
         * Checks if the property path includes an index.
         *
         * @return true if an index is present and not empty, false otherwise.
         */
        public boolean isIndexed() {
            return index != null && !index.isEmpty();
        }
    }

    /**
     * A specialized {@code ValuePath} that handles retrieval of values from a {@code Map} object.
     * It extends {@code ValuePath} to override the {@code get} method for cases when the target
     * is a {@code Map}. If the target is not a {@code Map}, or the path is not found within it,
     * it falls back to the default implementation provided by the {@code ValuePath} class.
     */
    public static class MapPath extends ValuePath {

        /**
         * Constructs a {@code MapPath} with the provided path.
         *
         * @param path The string path used to retrieve a value from a {@code Map}.
         */
        public MapPath(String path) {
            super(path);
        }

        /**
         * Constructs a {@code MapPath} with the provided path and a predicate for additional checks.
         *
         * @param path      The string path used to retrieve a value from a {@code Map}.
         * @param predicate A predicate to apply further filtering on the retrieved value.
         */
        public MapPath(String path, Predicate<Object> predicate) {
            super(path, predicate);
        }

        @Override
        public Object get(Object target) {
            if (target instanceof Map && path != null && !path.isEmpty()) {
                Object result = ((Map<?, ?>) target).get(path);
                if (result != null) {
                    return result;
                }
            }
            return super.get(target);
        }
    }
}
