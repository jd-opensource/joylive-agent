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

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

/**
 * Utility class providing a collection of helper methods for class manipulation and metadata access.
 */
public class ClassUtils {

    private static final Map<Class<?>, Class<?>> inboxes = new HashMap<>(10);
    /**
     * Cache for class metadata.
     */
    private final static Map<Class<?>, ClassDesc> classDescs = new ConcurrentHashMap<>(5000);

    static {
        // Mapping of primitive types to their corresponding wrapper classes.
        inboxes.put(long.class, Long.class);
        inboxes.put(int.class, Integer.class);
        inboxes.put(short.class, Short.class);
        inboxes.put(byte.class, Byte.class);
        inboxes.put(double.class, Double.class);
        inboxes.put(float.class, Float.class);
        inboxes.put(char.class, Character.class);
        inboxes.put(boolean.class, Boolean.class);
    }

    /**
     * Returns the wrapper class for a given primitive type, or the type itself if it's not a primitive.
     *
     * @param type The class to check and potentially "inbox".
     * @return The corresponding wrapper class if the given class is a primitive type, otherwise the class itself.
     */
    public static Class<?> inbox(Class<?> type) {
        return type == null || !type.isPrimitive() ? type : inboxes.getOrDefault(type, type);
    }

    /**
     * Retrieves the metadata description for a given class.
     *
     * @param type The class to describe.
     * @return The metadata description of the class.
     */
    public static ClassDesc describe(final Class<?> type) {
        return type == null ? null : classDescs.computeIfAbsent(type, ClassDesc::new);
    }

    /**
     * Retrieves the value of a field from the given target object.
     *
     * @param <T>       the type of the field value
     * @param target    the target object
     * @param fieldName the name of the field
     * @return the value of the field, or null if the field does not exist or is inaccessible
     */
    public static <T> T getValue(Object target, String fieldName) {
        return getValue(target, fieldName, null);
    }

    /**
     * Retrieves the value of a field from the given target object, applying an optional predicate to filter the result.
     *
     * @param <T>       the type of the field value
     * @param target    the target object
     * @param fieldName the name of the field
     * @param predicate an optional predicate to filter the result
     * @return the value of the field, or null if the field does not exist, is inaccessible, or does not pass the predicate test
     */
    @SuppressWarnings("unchecked")
    public static <T> T getValue(Object target, String fieldName, Predicate<Object> predicate) {
        if (target == null) {
            return null;
        }
        Object value = describe(target.getClass()).getValue(fieldName, target);
        return predicate == null || predicate.test(value) ? (T) value : null;
    }

    /**
     * Retrieves the value of a nested property from the given target object, applying an optional predicate to filter the result.
     *
     * @param <T>        the type of the property value
     * @param target     the target object
     * @param properties an array of property names, representing the path to the desired property
     * @param predicate  an optional predicate to filter the result
     * @return the value of the property, or null if the property does not exist, is inaccessible, or does not pass the predicate test
     */
    @SuppressWarnings("unchecked")
    public static <T> T getValue(Object target, String[] properties, Predicate<Object> predicate) {
        if (target == null || properties == null) {
            return null;
        }
        Object value;
        int pos;
        String name;
        for (String fieldName : properties) {
            value = target;
            while (fieldName != null && !fieldName.isEmpty() && value != null) {
                pos = fieldName.indexOf('.');
                name = pos > 0 ? fieldName.substring(0, pos) : fieldName;
                fieldName = pos > 0 ? fieldName.substring(pos + 1) : null;
                value = describe(value.getClass()).getValue(name, value);
            }
            if (value != null && (predicate == null || predicate.test(value))) {
                return (T) value;
            }
        }
        return null;
    }

    /**
     * Determines if the provided class is considered an "entity" according to custom rules defined in TypeScanner.
     *
     * @param type The class to check.
     * @return True if the class is considered an entity, false otherwise.
     */
    public static boolean isEntity(Class<?> type) {
        return TypeScanner.UNTIL_OBJECT.test(type);
    }

    /**
     * Load the class with the specified class name using the provided class loader.
     *
     * @param className   the name of the class to load
     * @param classLoader the class loader to use for loading the class
     * @return the loaded class, or null if the class cannot be loaded
     */
    public static Class<?> loadClass(String className, ClassLoader classLoader) {
        try {
            return classLoader.loadClass(className);
        } catch (Throwable e) {
            ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
            if (classLoader != contextClassLoader) {
                try {
                    return contextClassLoader.loadClass(className);
                } catch (Throwable ignored) {
                }
            }
            return null;
        }
    }

}


