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
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * A factory class that provides access to a specific field of an object.
 */
public class UnsafeFieldAccessorFactory {

    private static Function<Field, UnsafeFieldAccessor> unsafeFieldFunc;

    private static BiFunction<Class<?>, String, UnsafeFieldAccessor> unsafeNameFunc;

    static {
        ClassLoader classLoader = ClassLoader.getSystemClassLoader();
        try {
            classLoader.loadClass("jdk.internal.misc.Unsafe");
            jdk.internal.misc.Unsafe unsafe = jdk.internal.misc.Unsafe.getUnsafe();
            boolean hasReference = withReference(unsafe);
            unsafeFieldFunc = field -> new OpenUnsafeFieldAccessor(field, unsafe, hasReference);
            unsafeNameFunc = (type, name) -> new OpenUnsafeFieldAccessor(type, name, unsafe, hasReference);
            System.out.println("Use jdk.internal.misc.Unsafe to fast access field");
        } catch (Throwable e) {
            try {
                classLoader.loadClass("sun.misc.Unsafe");
                Field field = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
                field.setAccessible(true);
                sun.misc.Unsafe unsafe = (sun.misc.Unsafe) field.get(null);
                unsafeFieldFunc = f -> new MiscUnsafeFieldAccessor(f, unsafe);
                System.out.println("Use sun.misc.Unsafe to fast access field");
            } catch (Throwable ignore) {
                System.out.println("Unable to use unsafe to fast access field");
            }
        }
    }

    /**
     * Returns an {@link UnsafeFieldAccessor} for the specified field.
     *
     * @param field the field to access
     * @return an {@link UnsafeFieldAccessor} for the specified field
     */
    public static UnsafeFieldAccessor getAccessor(Field field) {
        return getAccessor(field, ReflectFieldAccessor::new);
    }

    /**
     * Returns an {@link UnsafeFieldAccessor} for the specified field.
     *
     * @param field       the field to access
     * @param defaultFunc a function to create a default accessor if no other accessor is available
     * @return an {@link UnsafeFieldAccessor} for the specified field
     */
    public static UnsafeFieldAccessor getAccessor(Field field, Function<Field, UnsafeFieldAccessor> defaultFunc) {
        if (field == null) {
            return null;
        }
        Function<Field, UnsafeFieldAccessor> func = unsafeFieldFunc == null ? defaultFunc : unsafeFieldFunc;
        return func == null ? null : func.apply(field);
    }

    /**
     * Returns a {@link UnsafeFieldAccessor} object for the specified field in the given class.
     *
     * @param clazz the class containing the field
     * @param field the name of the field
     * @return a {@link UnsafeFieldAccessor} object for the specified field
     * @throws NoSuchFieldException if the specified field does not exist in the given class
     */
    public static UnsafeFieldAccessor getAccessor(Class<?> clazz, String field) throws NoSuchFieldException {
        return getAccessor(clazz, field, ReflectFieldAccessor::new);
    }

    /**
     * Returns a {@link UnsafeFieldAccessor} object for the specified field in the given class.
     *
     * @param clazz the class containing the field
     * @param field the name of the field
     * @return a {@link UnsafeFieldAccessor} object for the specified field
     */
    public static UnsafeFieldAccessor getQuietly(Class<?> clazz, String field) {
        try {
            return getAccessor(clazz, field, ReflectFieldAccessor::new);
        } catch (NoSuchFieldException e) {
            return null;
        }
    }

    /**
     * Returns a {@link UnsafeFieldAccessor} object for the specified field in the given class.
     *
     * @param clazz       the class containing the field
     * @param field       the name of the field
     * @param defaultFunc a function to create a default accessor if no other accessor is available
     * @return a {@link UnsafeFieldAccessor} object for the specified field
     * @throws NoSuchFieldException if the specified field does not exist in the given class
     */
    public static UnsafeFieldAccessor getAccessor(Class<?> clazz, String field, Function<Field, UnsafeFieldAccessor> defaultFunc) throws NoSuchFieldException {
        if (clazz == null || field == null || field.isEmpty()) {
            return null;
        }
        if (unsafeNameFunc != null) {
            try {
                return unsafeNameFunc.apply(clazz, field);
            } catch (InternalError e) {
                throw new NoSuchFieldException("Field " + field + " is not found");
            }
        }
        return getAccessor(clazz.getDeclaredField(field), defaultFunc);
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
    public static <T> T getQuietly(Object target, String field) {
        return (T) getQuietly(target, field, null);
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
        if (target == null || field == null || field.isEmpty()) {
            return null;
        }
        try {
            UnsafeFieldAccessor accessor = getAccessor(target.getClass(), field);
            return accessor == null ? null : (T) accessor.get(target, predicate);
        } catch (NoSuchFieldException e) {
            return null;
        }
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

    /**
     * A static inner class that implements the FieldAccessor interface using reflection.
     */
    @Getter
    private static class ReflectFieldAccessor implements UnsafeFieldAccessor {

        private final Field field;

        ReflectFieldAccessor(Field field) {
            this.field = field;
            field.setAccessible(true);
        }

        @Override
        public Object get(Object target) {
            try {
                return field.get(target);
            } catch (IllegalAccessException e) {
                throw new ReflectException("an error occurred while getting field value. " + field.getName(), e);
            }
        }

        @Override
        public void set(Object target, Object value) {
            try {
                field.set(target, value);
            } catch (IllegalAccessException e) {
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

        OpenUnsafeFieldAccessor(Class<?> type, String field, jdk.internal.misc.Unsafe unsafe, boolean hasReference) {
            this.unsafe = unsafe;
            this.offset = unsafe.objectFieldOffset(type, field);
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
