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
package com.jd.live.agent.core.inject.jbind;

import lombok.Getter;

import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.Collection;

/**
 * The {@code TypeInfo} class provides information about the type of a class, including whether it is
 * a collection, a character sequence, a primitive, an array, an enum, abstract, interface, or final.
 * It encapsulates both the raw type and an optional inner box type, as well as the generic type.
 */
@Getter
public class TypeInfo {

    private final Class<?> rawType;  // The raw (non-parameterized) type of the class
    private final Class<?> inboxType; // The inner boxed type if applicable, otherwise same as rawType
    private final Type type;         // The actual type, which may include generic type information

    /**
     * Constructs a new {@code TypeInfo} instance with the specified raw type, inner box type, and type.
     *
     * @param rawType   the raw type of the class
     * @param inboxType the inner boxed type of the class, if applicable
     * @param type      the actual type, which may include generic type information
     */
    public TypeInfo(Class<?> rawType, Class<?> inboxType, Type type) {
        this.rawType = rawType;
        this.inboxType = inboxType;
        this.type = type;
    }

    /**
     * Constructs a new {@code TypeInfo} instance with the specified raw type and inner box type,
     * using the inner box type for both parameters.
     *
     * @param rawType   the raw type of the class
     * @param inboxType the inner boxed type of the class
     */
    public TypeInfo(Class<?> rawType, Class<?> inboxType) {
        this(rawType, inboxType, inboxType);
    }

    /**
     * Determines if the raw type represents a collection.
     *
     * @return true if the raw type is assignable from {@code Collection}, false otherwise
     */
    public boolean isCollection() {
        return Collection.class.isAssignableFrom(rawType);
    }

    /**
     * Determines if the raw type represents a character sequence.
     *
     * @return true if the raw type is assignable from {@code CharSequence}, false otherwise
     */
    public boolean isCharSequence() {
        return CharSequence.class.isAssignableFrom(rawType);
    }

    /**
     * Determines if the raw type is a primitive type.
     *
     * @return true if the raw type is a primitive, false otherwise
     */
    public boolean isPrimitive() {
        return rawType.isPrimitive();
    }

    /**
     * Determines if the raw type represents an array.
     *
     * @return true if the raw type is an array, false otherwise
     */
    public boolean isArray() {
        return rawType.isArray();
    }

    /**
     * Determines if the raw type represents an enum.
     *
     * @return true if the raw type is an enum, false otherwise
     */
    public boolean isEnum() {
        return rawType.isEnum();
    }

    /**
     * Determines if the raw type is declared as abstract.
     *
     * @return true if the raw type is abstract, false otherwise
     */
    public boolean isAbstract() {
        return Modifier.isAbstract(rawType.getModifiers());
    }

    /**
     * Determines if the raw type is an interface.
     *
     * @return true if the raw type is an interface, false otherwise
     */
    public boolean isInterface() {
        return rawType.isInterface();
    }

    /**
     * Determines if the raw type is declared as final.
     *
     * @return true if the raw type is final, false otherwise
     */
    public boolean isFinal() {
        return Modifier.isFinal(rawType.getModifiers());
    }
}

