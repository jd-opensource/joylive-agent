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

import com.jd.live.agent.bootstrap.exception.ReflectException;
import com.jd.live.agent.core.util.cache.LazyObject;
import com.jd.live.agent.core.util.type.generic.Generic;
import lombok.Getter;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/**
 * A descriptor for accessing fields of an object via reflection.
 * This class encapsulates the reflective access to a field, including its getter and setter methods,
 * and provides additional metadata about the field.
 */
public class FieldDesc implements ObjectAccessor {
    /**
     * The class that owns the field.
     */
    @Getter
    private final Class<?> owner;
    /**
     * The field being described.
     */
    @Getter
    private final Field field;
    /**
     * A lazily-initialized getter method for the field.
     */
    private final LazyObject<Method> getter;
    /**
     * A lazily-initialized setter method for the field.
     */
    private final LazyObject<Method> setter;
    /**
     * The generic type information of the field.
     */
    private final LazyObject<Generic> generic;

    /**
     * Constructs a FieldDesc instance using a field and a supplier for getter, setter, and generic type.
     *
     * @param owner    The class that owns the field.
     * @param field    The field to describe.
     * @param supplier A supplier that provides getter, setter, and generic type information.
     */
    public FieldDesc(Class<?> owner, Field field, FieldSupplier supplier) {
        this.owner = owner;
        this.field = field;
        this.getter = LazyObject.of(() -> supplier == null ? null : supplier.getGetter(field));
        this.setter = LazyObject.of(() -> supplier == null ? null : supplier.getSetter(field));
        this.generic = LazyObject.of(() -> supplier == null ? null : supplier.getGeneric(field));
    }

    /**
     * Constructs a FieldDesc instance directly with specified getter, setter, and generic type.
     *
     * @param owner   The class that owns the field.
     * @param field   The field to describe.
     * @param getter  The getter method for the field.
     * @param setter  The setter method for the field.
     * @param generic The generic type information of the field.
     */
    public FieldDesc(Class<?> owner, Field field, Method getter, Method setter, Generic generic) {
        this.owner = owner;
        this.field = field;
        this.getter = LazyObject.of(getter);
        this.setter = LazyObject.of(setter);
        this.generic = LazyObject.of(generic);
    }

    /**
     * Retrieves the getter method for the field, if available.
     *
     * @return The getter method or {@code null}.
     */
    public Method getGetter() {
        return getter.get();
    }

    /**
     * Retrieves the setter method for the field, if available.
     *
     * @return The setter method or {@code null}.
     */
    public Method getSetter() {
        return setter.get();
    }

    /**
     * Retrieves the generic type information for the field, if available.
     *
     * @return The generic type information or {@code null}.
     */
    public Generic getGeneric() {
        return generic.get();
    }

    /**
     * Retrieves the name of the field.
     *
     * @return The field name.
     */
    public String getName() {
        return field.getName();
    }

    /**
     * Retrieves a specific annotation from the field, if present.
     *
     * @param type The class of the annotation to retrieve.
     * @param <T>  The type of the annotation.
     * @return The annotation instance, or {@code null} if not present.
     */
    public <T extends Annotation> T getAnnotation(Class<T> type) {
        return field.getAnnotation(type);
    }

    /**
     * Retrieves all annotations present on the field.
     *
     * @return An array of annotations.
     */
    public Annotation[] getAnnotations() {
        return field.getAnnotations();
    }

    /**
     * Retrieves the type of the field, considering generic type if available.
     *
     * @return The field's type.
     */
    public Class<?> getType() {
        Generic gen = getGeneric();
        return gen != null ? gen.getErasure() : field.getType();
    }

    /**
     * Retrieves the modifiers of the field.
     *
     * @return The field's modifiers.
     */
    public int getModifiers() {
        return field.getModifiers();
    }

    /**
     * Checks if the field is writable.
     *
     * @return {@code true} if the field is writable, {@code false} otherwise.
     */
    public boolean isWriteable() {
        return (field != null && !Modifier.isFinal(field.getModifiers())) || setter != null;
    }

    /**
     * Checks if the field is readable.
     *
     * @return {@code true} if the field is readable, {@code false} otherwise.
     */
    public boolean isReadable() {
        return field != null && getter != null;
    }

    /**
     * Retrieves the value of the field from a target object.
     *
     * @param target The object from which to retrieve the field's value.
     * @return The value of the field.
     */
    @Override
    public Object get(final Object target) {
        if (target == null && (field.getModifiers() & Modifier.STATIC) == 0) {
            // none static field with null target
            return null;
        }
        try {
            Method getter = getGetter();
            if (getter != null) {
                return getter.invoke(target);
            } else if (field != null) {
                if (!field.isAccessible()) {
                    field.setAccessible(true);
                }
                return field.get(target);
            }
            return null;
        } catch (Exception e) {
            throw new ReflectException("an error occurred while getting field value. " + field.getName(), e);
        }

    }

    /**
     * Sets the value of the field on a target object.
     *
     * @param target The object on which to set the field's value.
     * @param value  The new value for the field.
     */
    @Override
    public void set(final Object target, final Object value) {
        if (target == null) {
            return;
        }
        try {
            Method setter = getSetter();
            if (setter != null) {
                if (!setter.isAccessible())
                    setter.setAccessible(true);
                setter.invoke(target, value);
            } else if (field != null) {
                if (!field.isAccessible()) {
                    field.setAccessible(true);
                }
                field.set(target, value);
            }
        } catch (Exception e) {
            throw new ReflectException("an error occurred while setting field value. " + field.getName(), e);
        }
    }

}
