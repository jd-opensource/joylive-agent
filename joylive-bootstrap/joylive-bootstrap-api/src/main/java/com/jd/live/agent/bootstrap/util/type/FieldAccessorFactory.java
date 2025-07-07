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
package com.jd.live.agent.bootstrap.util.type;

import com.jd.live.agent.bootstrap.exception.ReflectException;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * A factory class that provides access to a specific field of an object.
 */
public class FieldAccessorFactory {

    private static final Map<Class<?>, Map<String, Optional<Field>>> fields = new ConcurrentHashMap<>();

    private static final Function<Field, FieldAccessor> unsafeFieldFunc;

    private static final Map<Class<?>, UnsafeGetSetter> unsafeGetSetters = new ConcurrentHashMap<>();

    private static final UnsafeGetSetter defaultUnsafeGetSetter = new UnsafeGetSetter(
            (o, accessor) -> accessor.get(o),
            (o, v, accessor) -> accessor.set(o, v)
    );

    static {
        unsafeFieldFunc = getUnsafe();

        unsafeGetSetters.put(int.class, new UnsafeGetSetter(
                (o, accessor) -> accessor.getInt(o),
                (o, v, accessor) -> accessor.setInt(o, (int) v)));
        unsafeGetSetters.put(long.class, new UnsafeGetSetter(
                (o, accessor) -> accessor.getLong(o),
                (o, v, accessor) -> accessor.setLong(o, (long) v)));
        unsafeGetSetters.put(short.class, new UnsafeGetSetter(
                (o, accessor) -> accessor.getShort(o),
                (o, v, accessor) -> accessor.setShort(o, (short) v)));
        unsafeGetSetters.put(byte.class, new UnsafeGetSetter(
                (o, accessor) -> accessor.getByte(o),
                (o, v, accessor) -> accessor.setByte(o, (byte) v)));
        unsafeGetSetters.put(double.class, new UnsafeGetSetter(
                (o, accessor) -> accessor.getDouble(o),
                (o, v, accessor) -> accessor.setDouble(o, (double) v)));
        unsafeGetSetters.put(float.class, new UnsafeGetSetter(
                (o, accessor) -> accessor.getFloat(o),
                (o, v, accessor) -> accessor.setFloat(o, (float) v)));
        unsafeGetSetters.put(boolean.class, new UnsafeGetSetter(
                (o, accessor) -> accessor.getBoolean(o),
                (o, v, accessor) -> accessor.setBoolean(o, (boolean) v)));
        unsafeGetSetters.put(char.class, new UnsafeGetSetter(
                (o, accessor) -> accessor.getChar(o),
                (o, v, accessor) -> accessor.setChar(o, (char) v)));
    }

    /**
     * Provides unsafe field accessors using different implementations (JDK Unsafe, Sun Unsafe, or reflection fallback).
     * 1. jdk.internal.misc.Unsafe (modern JDKs)
     * 2. sun.misc.Unsafe (legacy JDKs)
     * 3. Reflection (fallback)
     *
     * @return Function that creates field accessors for the best available implementation
     */
    protected static Function<Field, FieldAccessor> getUnsafe() {
        ClassLoader classLoader = ClassLoader.getSystemClassLoader();
        Function<Field, FieldAccessor> result;
        try {
            result = getJDKUnsafe(classLoader);
        } catch (Throwable e) {
            try {
                result = getSunUnsafe(classLoader);
            } catch (Throwable ex) {
                result = ReflectFieldAccessor::new;
            }
        }
        return result;
    }

    /**
     * Creates JDK's internal Unsafe-based field accessor.
     *
     * @param classLoader ClassLoader to load Unsafe class
     * @return Unsafe accessor function
     * @throws ClassNotFoundException if jdk.internal.misc.Unsafe is unavailable
     */
    protected static Function<Field, FieldAccessor> getJDKUnsafe(ClassLoader classLoader) throws ClassNotFoundException, NoSuchFieldException, IllegalAccessException {
        Class<?> type = classLoader.loadClass("jdk.internal.misc.Unsafe");
        Field field = type.getDeclaredField("theUnsafe");
        field.setAccessible(true);
        jdk.internal.misc.Unsafe unsafe = (jdk.internal.misc.Unsafe) field.get(null);
        boolean hasReference = withReference(unsafe);
        return f -> new UnsafeFieldAccessor(f, new OpenUnsafeObjectAccessor(unsafe, unsafe.objectFieldOffset(f), hasReference));
    }

    /**
     * Creates Sun's legacy Unsafe-based field accessor.
     *
     * @param classLoader ClassLoader to load Unsafe class
     * @return Unsafe accessor function
     * @throws ClassNotFoundException if sun.misc.Unsafe is unavailable
     * @throws NoSuchFieldException   if Unsafe instance field not found
     * @throws IllegalAccessException if access to Unsafe is denied
     */
    protected static Function<Field, FieldAccessor> getSunUnsafe(ClassLoader classLoader) throws ClassNotFoundException, NoSuchFieldException, IllegalAccessException {
        Class<?> type = classLoader.loadClass("sun.misc.Unsafe");
        Field field = type.getDeclaredField("theUnsafe");
        field.setAccessible(true);
        sun.misc.Unsafe unsafe = (sun.misc.Unsafe) field.get(null);
        return f -> new UnsafeFieldAccessor(f, new MiscUnsafeObjectAccessor(unsafe, unsafe.objectFieldOffset(field)));
    }

    /**
     * Returns an {@link FieldAccessor} for the specified field.
     *
     * @param field the field to access
     * @return an {@link FieldAccessor} for the specified field
     */
    public static FieldAccessor getAccessor(Field field) {
        return field == null ? null : unsafeFieldFunc.apply(field);
    }

    /**
     * Returns a {@link FieldAccessor} object for the specified field in the given class.
     *
     * @param clazz the class containing the field
     * @param field the name of the field
     * @return a {@link FieldAccessor} object for the specified field
     */
    public static FieldAccessor getAccessor(Class<?> clazz, String field) {
        return getAccessor(getField(clazz, field));
    }

    /**
     * Retrieves the value of the specified field from the target object quietly.
     * If the target object or the field name is null or empty, or if the field does not exist, returns null.
     *
     * @param target The target object from which to retrieve the field value.
     * @param field  The name of the field to retrieve.
     * @return The value of the specified field, or null if the field does not exist or the target object is null.
     */
    @SuppressWarnings("unchecked")
    public static <T> T getQuietly(Object target, Field field) {
        if (target == null || field == null) {
            return null;
        }
        FieldAccessor accessor = getAccessor(field);
        return (T) accessor.get(target);
    }

    /**
     * Retrieves the value of the specified field from the target object quietly.
     * If the target object or the field name is null or empty, or if the field does not exist, returns null.
     *
     * @param target The target object from which to retrieve the field value.
     * @param field  The name of the field to retrieve.
     * @return The value of the specified field, or null if the field does not exist or the target object is null.
     */
    public static <T> T getQuietly(Object target, String field) {
        return getQuietly(target, field, null);
    }

    /**
     * Retrieves the value of the specified field from the target object quietly, filtered by the given predicate.
     * If the target object or the field name is null or empty, or if the field does not exist, returns null.
     *
     * @param <T>       The type of the field value.
     * @param target    The target object from which to retrieve the field value.
     * @param field     The name of the field to retrieve.
     * @param predicate A predicate to filter the field value. If the predicate returns false, the method returns null.
     * @return The value of the specified field, or null if the field does not exist, the target object is null, or the predicate returns false.
     */
    @SuppressWarnings("unchecked")
    public static <T> T getQuietly(Object target, String field, Predicate<Object> predicate) {
        if (target == null) {
            return null;
        }
        Field f = getField(target.getClass(), field);
        if (f == null) {
            return null;
        }
        FieldAccessor accessor = getAccessor(f);
        Object result = accessor.get(target);
        return result != null && (predicate == null || predicate.test(result)) ? (T) result : null;
    }

    /**
     * Safely gets a field value from an object using specified access strategy.
     *
     * @param target    The object instance to read from (null returns null)
     * @param field     The field name to access (null/empty returns null)
     * @param function  Field accessor factory function (null returns null)
     * @param predicate Optional validation predicate for the return value
     * @param <T>       Expected return type
     * @return Field value if found and valid, otherwise null
     */
    @SuppressWarnings("unchecked")
    protected static <T> T getQuietly(Object target,
                                      String field,
                                      Function<Field, FieldAccessor> function,
                                      Predicate<Object> predicate) {
        if (target == null || field == null || field.isEmpty() || function == null) {
            return null;
        }
        Field f = getField(target.getClass(), field);
        if (f == null) {
            return null;
        }
        FieldAccessor accessor = function.apply(f);
        Object result = accessor.get(target);
        return result != null && (predicate == null || predicate.test(result)) ? (T) result : null;
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
    public static <T> T getQuietly(Object target, String[] properties, Predicate<Object> predicate) {
        if (target == null || properties == null || properties.length == 0) {
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
                value = getQuietly(value, name);
            }
            if (value != null && (predicate == null || predicate.test(value))) {
                return (T) value;
            }
        }
        return null;
    }

    /**
     * Sets a field value on the target object using field name lookup.
     *
     * @param target The object instance to modify
     * @param field  The name of the field to set
     * @param value  The value to set
     */
    public static void setValue(Object target, String field, Object value) {
        if (target == null || field == null) {
            return;
        }
        setValue(target, getField(target.getClass(), field), value, unsafeFieldFunc);
    }

    /**
     * Sets a field value by name on the target object.
     * Silently skips if any parameter is null.
     *
     * @param target       the object instance to modify
     * @param field        the name of the field to set
     * @param value        the new value to assign
     * @param accessorFunc function that provides field access
     */
    protected static void setValue(Object target, String field, Object value, Function<Field, FieldAccessor> accessorFunc) {
        if (target == null || field == null || accessorFunc == null) {
            return;
        }
        setValue(target, getField(target.getClass(), field), value, accessorFunc);
    }

    /**
     * Sets a field value directly using a pre-resolved Field object.
     *
     * @param target The object instance to modify
     * @param field  The specific Field to modify
     * @param value  The value to set
     */
    public static void setValue(Object target, Field field, Object value) {
        setValue(target, field, value, unsafeFieldFunc);
    }

    /**
     * Sets a field value on the target object using an unsafe accessor.
     * Silently skips if any required parameter is null.
     *
     * @param target       the object whose field will be modified
     * @param field        the field to set
     * @param value        the new value to assign
     * @param accessorFunc function that provides the field accessor
     */
    protected static void setValue(Object target, Field field, Object value, Function<Field, FieldAccessor> accessorFunc) {
        if (target == null || field == null || accessorFunc == null) {
            return;
        }
        FieldAccessor accessor = accessorFunc.apply(field);
        accessor.set(target, value);
    }

    /**
     * Finds a field by name in a class hierarchy using cached lookups.
     *
     * @param clazz The class to start searching from (may be null)
     * @param field The field name to find (null/empty returns null)
     * @return The found Field object, or null if not found
     * @implNote Performs thread-safe caching of field lookups.
     * Searches through the entire class hierarchy including superclasses.
     * Returns the first matching field found in the hierarchy.
     */
    private static Field getField(Class<?> clazz, String field) {
        if (clazz == null || field == null || field.isEmpty()) {
            return null;
        }
        Map<String, Optional<Field>> map = fields.computeIfAbsent(clazz, k -> new ConcurrentHashMap<>());
        return map.computeIfAbsent(field, k -> {
            Class<?> parent = clazz;
            while (parent != null && parent != Object.class) {
                try {
                    Field f = parent.getDeclaredField(k);
                    f.setAccessible(true);
                    return Optional.of(f);
                } catch (NoSuchFieldException e) {
                    parent = parent.getSuperclass();
                }
            }
            return Optional.empty();
        }).orElse(null);
    }

    /**
     * Checks if the given object has a putReference method.
     *
     * @param unsafe The object to check.
     * @return true if the object has a putReference method, false otherwise.
     */
    private static boolean withReference(Object unsafe) {
        try {
            unsafe.getClass().getMethod("putReference", Object.class, long.class, Object.class);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    @FunctionalInterface
    private interface UnsafeSetter {
        void set(Object target, Object value, UnsafeObjectAccessor accessor);
    }

    @FunctionalInterface
    private interface UnsafeGetter {
        Object get(Object target, UnsafeObjectAccessor accessor);
    }

    @Getter
    @AllArgsConstructor
    private static class UnsafeGetSetter {
        private UnsafeGetter getter;
        private UnsafeSetter setter;
    }

    /**
     * An interface that provides access to a specific field of an object using the Unsafe API.
     *
     * @see ObjectAccessor
     */
    private interface UnsafeObjectAccessor extends ObjectAccessor {

        /**
         * Returns the value of the field as a boolean for the specified object.
         *
         * @param object the object to access
         * @return the value of the field as a boolean
         */
        boolean getBoolean(Object object);

        /**
         * Sets the value of the field to the specified boolean value for the specified object.
         *
         * @param target the object to access
         * @param value  the new value of the field
         */
        void setBoolean(Object target, boolean value);

        /**
         * Returns the value of the field as a char for the specified object.
         *
         * @param object the object to access
         * @return the value of the field as a char
         */
        char getChar(Object object);

        /**
         * Sets the value of the field to the specified char value for the specified object.
         *
         * @param target the object to access
         * @param value  the new value of the field
         */
        void setChar(Object target, char value);

        /**
         * Returns the value of the field as a byte for the specified object.
         *
         * @param object the object to access
         * @return the value of the field as a byte
         */
        byte getByte(Object object);

        /**
         * Sets the value of the field to the specified byte value for the specified object.
         *
         * @param target the object to access
         * @param value  the new value of the field
         */
        void setByte(Object target, byte value);

        /**
         * Returns the value of the field as a short for the specified object.
         *
         * @param object the object to access
         * @return the value of the field as a short
         */
        short getShort(Object object);

        /**
         * Sets the value of the field to the specified short value for the specified object.
         *
         * @param target the object to access
         * @param value  the new value of the field
         */
        void setShort(Object target, short value);

        /**
         * Returns the value of the field as an int for the specified object.
         *
         * @param object the object to access
         * @return the value of the field as an int
         */
        int getInt(Object object);

        /**
         * Sets the value of the field to the specified int value for the specified object.
         *
         * @param target the object to access
         * @param value  the new value of the field
         */
        void setInt(Object target, int value);

        /**
         * Returns the value of the field as a long for the specified object.
         *
         * @param object the object to access
         * @return the value of the field as a long
         */
        long getLong(Object object);

        /**
         * Sets the value of the field to the specified long value for the specified object.
         *
         * @param target the object to access
         * @param value  the new value of the field
         */
        void setLong(Object target, long value);

        /**
         * Returns the value of the field as a float for the specified object.
         *
         * @param object the object to access
         * @return the value of the field as a float
         */
        float getFloat(Object object);

        /**
         * Sets the value of the field to the specified float value for the specified object.
         *
         * @param target the object to access
         * @param value  the new value of the field
         */
        void setFloat(Object target, float value);

        /**
         * Returns the value of the field as a double for the specified object.
         *
         * @param object the object to access
         * @return the value of the field as a double
         */
        double getDouble(Object object);

        /**
         * Sets the value of the field to the specified double value for the specified object.
         *
         * @param target the object to access
         * @param value  the new value of the field
         */
        void setDouble(Object target, double value);
    }

    /**
     * A static inner class that implements the FieldAccessor interface using the sun.misc.Unsafe API.
     */
    private static class MiscUnsafeObjectAccessor implements UnsafeObjectAccessor {

        private final sun.misc.Unsafe unsafe;

        private final long offset;

        MiscUnsafeObjectAccessor(sun.misc.Unsafe unsafe, long offset) {
            this.unsafe = unsafe;
            this.offset = offset;
        }

        @Override
        public Object get(Object target) {
            return unsafe.getObject(target, offset);
        }

        @Override
        public void set(Object target, Object value) {
            unsafe.putObject(target, offset, value);
        }

        @Override
        public boolean getBoolean(Object object) {
            return unsafe.getBoolean(object, offset);
        }

        @Override
        public void setBoolean(Object target, boolean value) {
            unsafe.putBoolean(target, offset, value);
        }

        @Override
        public char getChar(Object object) {
            return unsafe.getChar(object, offset);
        }

        @Override
        public void setChar(Object target, char value) {
            unsafe.putChar(target, offset, value);
        }

        @Override
        public byte getByte(Object object) {
            return unsafe.getByte(object, offset);
        }

        @Override
        public void setByte(Object target, byte value) {
            unsafe.putByte(target, offset, value);
        }

        @Override
        public short getShort(Object object) {
            return unsafe.getShort(object, offset);
        }

        @Override
        public void setShort(Object target, short value) {
            unsafe.putShort(target, offset, value);
        }

        @Override
        public int getInt(Object object) {
            return unsafe.getInt(object, offset);
        }

        @Override
        public void setInt(Object target, int value) {
            unsafe.putInt(target, offset, value);
        }

        @Override
        public long getLong(Object object) {
            return unsafe.getLong(object, offset);
        }

        @Override
        public void setLong(Object target, long value) {
            unsafe.putLong(target, offset, value);
        }

        @Override
        public float getFloat(Object object) {
            return unsafe.getFloat(object, offset);
        }

        @Override
        public void setFloat(Object target, float value) {
            unsafe.putFloat(target, offset, value);
        }

        @Override
        public double getDouble(Object object) {
            return unsafe.getDouble(object, offset);
        }

        @Override
        public void setDouble(Object target, double value) {
            unsafe.putDouble(target, offset, value);
        }

    }

    /**
     * A static inner class that implements the FieldAccessor interface using the jdk.internal.misc.Unsafe API.
     */
    private static class OpenUnsafeObjectAccessor implements UnsafeObjectAccessor {

        private final jdk.internal.misc.Unsafe unsafe;

        private final long offset;

        private final boolean hasReference;

        OpenUnsafeObjectAccessor(jdk.internal.misc.Unsafe unsafe, long offset, boolean hasReference) {
            this.unsafe = unsafe;
            this.offset = offset;
            this.hasReference = hasReference;
        }

        @Override
        public Object get(Object target) {
            return hasReference ? unsafe.getReference(target, offset) : unsafe.getObject(target, offset);
        }

        @Override
        public void set(Object target, Object value) {
            if (hasReference) {
                unsafe.putReference(target, offset, value);
            } else {
                unsafe.putObject(target, offset, value);
            }
        }

        @Override
        public boolean getBoolean(Object object) {
            return unsafe.getBoolean(object, offset);
        }

        @Override
        public void setBoolean(Object target, boolean value) {
            unsafe.putBoolean(target, offset, value);
        }

        @Override
        public char getChar(Object object) {
            return unsafe.getChar(object, offset);
        }

        @Override
        public void setChar(Object target, char value) {
            unsafe.putChar(target, offset, value);
        }

        @Override
        public byte getByte(Object object) {
            return unsafe.getByte(object, offset);
        }

        @Override
        public void setByte(Object target, byte value) {
            unsafe.putByte(target, offset, value);
        }

        @Override
        public short getShort(Object object) {
            return unsafe.getShort(object, offset);
        }

        @Override
        public void setShort(Object target, short value) {
            unsafe.putShort(target, offset, value);
        }

        @Override
        public int getInt(Object object) {
            return unsafe.getInt(object, offset);
        }

        @Override
        public void setInt(Object target, int value) {
            unsafe.putInt(target, offset, value);
        }

        @Override
        public long getLong(Object object) {
            return unsafe.getLong(object, offset);
        }

        @Override
        public void setLong(Object target, long value) {
            unsafe.putLong(target, offset, value);
        }

        @Override
        public float getFloat(Object object) {
            return unsafe.getFloat(object, offset);
        }

        @Override
        public void setFloat(Object target, float value) {
            unsafe.putFloat(target, offset, value);
        }

        @Override
        public double getDouble(Object object) {
            return unsafe.getDouble(object, offset);
        }

        @Override
        public void setDouble(Object target, double value) {
            unsafe.putDouble(target, offset, value);
        }

    }

    /**
     * A static inner class that implements the FieldAccessor interface using reflection.
     */
    @Getter
    protected static class ReflectFieldAccessor implements FieldAccessor {

        private final Field field;

        ReflectFieldAccessor(Field field) {
            this.field = field;
            field.setAccessible(true);
        }

        @Override
        public Object get(Object target) {
            try {
                return field.get(target);
            } catch (Throwable e) {
                throw new ReflectException("an error occurred while getting field value. " + field.getName(), e);
            }
        }

        @Override
        public void set(Object target, Object value) {
            try {
                field.set(target, value);
            } catch (Throwable e) {
                throw new ReflectException("an error occurred while setting field value. " + field.getName(), e);
            }
        }

    }

    /**
     * Unsafe-based implementation of field access using {@link jdk.internal.misc.Unsafe}.
     * Provides get/set operations for fields through direct memory manipulation.
     */
    private static class UnsafeFieldAccessor implements FieldAccessor {

        @Getter
        private final Field field;

        private final UnsafeObjectAccessor objectAccessor;

        private final UnsafeGetSetter unsafeGetSetter;

        UnsafeFieldAccessor(Field field, UnsafeObjectAccessor objectAccessor) {
            this.field = field;
            this.objectAccessor = objectAccessor;
            this.unsafeGetSetter = unsafeGetSetters.getOrDefault(field.getType(), defaultUnsafeGetSetter);
        }

        @Override
        public Object get(Object target) {
            return unsafeGetSetter.getter.get(target, objectAccessor);
        }

        @Override
        public void set(Object target, Object value) {
            unsafeGetSetter.setter.set(target, value, objectAccessor);
        }

    }
}
