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
import lombok.Getter;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * A factory class that provides access to a specific field of an object.
 */
public class UnsafeFieldAccessorFactory {

    private static final Map<Class<?>, Map<String, Optional<Field>>> fields = new ConcurrentHashMap<>();

    private static final Function<Field, UnsafeFieldAccessor> unsafeFieldFunc;

    private static final Map<Class<?>, BiFunction<Object, UnsafeFieldAccessor, Object>> getters = new ConcurrentHashMap<>();

    private static final Map<Class<?>, ValueSetter> setters = new ConcurrentHashMap<>();

    static {
        unsafeFieldFunc = getUnsafe();

        getters.put(int.class, (o, accessor) -> accessor.getInt(o));
        getters.put(long.class, (o, accessor) -> accessor.getLong(o));
        getters.put(short.class, (o, accessor) -> accessor.getShort(o));
        getters.put(byte.class, (o, accessor) -> accessor.getByte(o));
        getters.put(double.class, (o, accessor) -> accessor.getDouble(o));
        getters.put(float.class, (o, accessor) -> accessor.getFloat(o));
        getters.put(boolean.class, (o, accessor) -> accessor.getBoolean(o));
        getters.put(char.class, (o, accessor) -> accessor.getChar(o));

        setters.put(int.class, (o, v, accessor) -> accessor.setInt(o, (int) v));
        setters.put(long.class, (o, v, accessor) -> accessor.setLong(o, (long) v));
        setters.put(short.class, (o, v, accessor) -> accessor.setShort(o, (short) v));
        setters.put(byte.class, (o, v, accessor) -> accessor.setByte(o, (byte) v));
        setters.put(double.class, (o, v, accessor) -> accessor.setDouble(o, (double) v));
        setters.put(float.class, (o, v, accessor) -> accessor.setFloat(o, (float) v));
        setters.put(boolean.class, (o, v, accessor) -> accessor.setBoolean(o, (boolean) v));
        setters.put(char.class, (o, v, accessor) -> accessor.setChar(o, (char) v));
    }

    /**
     * Provides unsafe field accessors using different implementations (JDK Unsafe, Sun Unsafe, or reflection fallback).
     * 1. jdk.internal.misc.Unsafe (modern JDKs)
     * 2. sun.misc.Unsafe (legacy JDKs)
     * 3. Reflection (fallback)
     *
     * @return Function that creates field accessors for the best available implementation
     */
    protected static Function<Field, UnsafeFieldAccessor> getUnsafe() {
        ClassLoader classLoader = ClassLoader.getSystemClassLoader();
        Function<Field, UnsafeFieldAccessor> result;
        try {
            result = getJDKUnsafe(classLoader);
            System.out.println("Use jdk.internal.misc.Unsafe to fast access field");
        } catch (Throwable e) {
            try {
                result = getSunUnsafe(classLoader);
                System.out.println("Use sun.misc.Unsafe to fast access field");
            } catch (Throwable ignore) {
                result = ReflectFieldAccessor::new;
                System.out.println("Use reflection to access field");
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
    protected static Function<Field, UnsafeFieldAccessor> getJDKUnsafe(ClassLoader classLoader) throws ClassNotFoundException {
        classLoader.loadClass("jdk.internal.misc.Unsafe");
        jdk.internal.misc.Unsafe unsafe = jdk.internal.misc.Unsafe.getUnsafe();
        boolean hasReference = withReference(unsafe);
        return field -> new OpenUnsafeFieldAccessor(field, unsafe, hasReference);
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
    protected static Function<Field, UnsafeFieldAccessor> getSunUnsafe(ClassLoader classLoader) throws ClassNotFoundException, NoSuchFieldException, IllegalAccessException {
        classLoader.loadClass("sun.misc.Unsafe");
        Field field = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
        field.setAccessible(true);
        sun.misc.Unsafe unsafe = (sun.misc.Unsafe) field.get(null);
        return f -> new MiscUnsafeFieldAccessor(f, unsafe);
    }

    /**
     * Returns an {@link UnsafeFieldAccessor} for the specified field.
     *
     * @param field the field to access
     * @return an {@link UnsafeFieldAccessor} for the specified field
     */
    public static UnsafeFieldAccessor getAccessor(Field field) {
        return field == null ? null : unsafeFieldFunc.apply(field);
    }

    /**
     * Returns a {@link UnsafeFieldAccessor} object for the specified field in the given class.
     *
     * @param clazz the class containing the field
     * @param field the name of the field
     * @return a {@link UnsafeFieldAccessor} object for the specified field
     */
    public static UnsafeFieldAccessor getAccessor(Class<?> clazz, String field) {
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
    public static <T> T getQuietly(Object target, Field field) {
        if (target == null || field == null) {
            return null;
        }
        UnsafeFieldAccessor accessor = getAccessor(field);
        return getQuietly(target, field, accessor, null);
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
    public static <T> T getQuietly(Object target, String field, Predicate<Object> predicate) {
        if (target == null) {
            return null;
        }
        Field f = getField(target.getClass(), field);
        if (f == null) {
            return null;
        }
        UnsafeFieldAccessor accessor = getAccessor(f);
        return getQuietly(target, f, accessor, predicate);
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
    protected static <T> T getQuietly(Object target,
                                      String field,
                                      Function<Field, UnsafeFieldAccessor> function,
                                      Predicate<Object> predicate) {
        if (target == null || field == null || field.isEmpty() || function == null) {
            return null;
        }
        Field f = getField(target.getClass(), field);
        if (f == null) {
            return null;
        }
        UnsafeFieldAccessor accessor = function.apply(f);
        return getQuietly(target, f, accessor, predicate);
    }

    /**
     * Low-level field value access with explicit field and accessor.
     *
     * @param target    The object instance to read from (null returns null)
     * @param field     The specific field to access (null returns null)
     * @param accessor  Pre-created field accessor (null returns null)
     * @param predicate Optional validation predicate for the return value
     * @param <T>       Expected return type
     * @return Field value if valid, otherwise null
     */
    @SuppressWarnings("unchecked")
    protected static <T> T getQuietly(Object target,
                                      Field field,
                                      UnsafeFieldAccessor accessor,
                                      Predicate<Object> predicate) {
        if (target == null || field == null || accessor == null) {
            return null;
        }
        BiFunction<Object, UnsafeFieldAccessor, Object> getter = getters.get(field.getType());
        Object result = getter == null ? accessor.get(target) : getter.apply(target, accessor);
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
        Field f = getField(target.getClass(), field);
        if (f == null) {
            return;
        }
        setValue(target, f, value);
    }

    /**
     * Sets a field value directly using a pre-resolved Field object.
     *
     * @param target The object instance to modify
     * @param field  The specific Field to modify
     * @param value  The value to set
     */
    public static void setValue(Object target, Field field, Object value) {
        if (target == null || field == null) {
            return;
        }
        UnsafeFieldAccessor accessor = getAccessor(field);

        ValueSetter setter = setters.get(field.getType());
        if (setter == null) {
            accessor.set(target, value);
        } else {
            setter.set(target, value, accessor);
        }
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
                    return Optional.of(parent.getDeclaredField(k));
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
    protected interface ValueSetter {

        void set(Object target, Object value, UnsafeFieldAccessor accessor);
    }

    /**
     * A static inner class that implements the FieldAccessor interface using reflection.
     */
    @Getter
    protected static class ReflectFieldAccessor implements UnsafeFieldAccessor {

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

        @Override
        public boolean getBoolean(Object object) {
            return (Boolean) get(object);
        }

        @Override
        public void setBoolean(Object target, boolean value) {
            set(target, value);
        }

        @Override
        public char getChar(Object object) {
            return (char) get(object);
        }

        @Override
        public void setChar(Object target, char value) {
            set(target, value);
        }

        @Override
        public byte getByte(Object object) {
            return (byte) get(object);
        }

        @Override
        public void setByte(Object target, byte value) {
            set(target, value);
        }

        @Override
        public short getShort(Object object) {
            return (short) get(object);
        }

        @Override
        public void setShort(Object target, short value) {
            set(target, value);
        }

        @Override
        public int getInt(Object object) {
            return (int) get(object);
        }

        @Override
        public void setInt(Object target, int value) {
            set(target, value);
        }

        @Override
        public long getLong(Object object) {
            return (long) get(object);
        }

        @Override
        public void setLong(Object target, long value) {
            set(target, value);
        }

        @Override
        public float getFloat(Object object) {
            return (float) get(object);
        }

        @Override
        public void setFloat(Object target, float value) {
            set(target, value);
        }

        @Override
        public double getDouble(Object object) {
            return (double) get(object);
        }

        @Override
        public void setDouble(Object target, double value) {
            set(target, value);
        }
    }

    /**
     * A static inner class that implements the FieldAccessor interface using the sun.misc.Unsafe API.
     */
    private static class MiscUnsafeFieldAccessor implements UnsafeFieldAccessor {

        private final sun.misc.Unsafe unsafe;

        private final long offset;

        MiscUnsafeFieldAccessor(Field field, sun.misc.Unsafe unsafe) {
            this.unsafe = unsafe;
            this.offset = unsafe.objectFieldOffset(field);
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
    private static class OpenUnsafeFieldAccessor implements UnsafeFieldAccessor {

        private final jdk.internal.misc.Unsafe unsafe;

        private final long offset;

        private final boolean hasReference;

        OpenUnsafeFieldAccessor(Field field, jdk.internal.misc.Unsafe unsafe, boolean hasReference) {
            this.unsafe = unsafe;
            this.offset = unsafe.objectFieldOffset(field);
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
}
