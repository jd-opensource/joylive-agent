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
package com.jd.live.agent.core.bytekit.type;

import java.util.List;

/**
 * Represents a definition of a type within the system, providing methods to inspect and
 * manipulate type information. This interface extends both {@code NamedElement} and
 * {@code MethodSource}, inheriting properties and methods related to names and methods respectively.
 *
 * @since 1.0.0
 */
public interface TypeDef extends NamedElement, MethodSource {

    /**
     * Determines whether this type definition represents an array type.
     *
     * @return true if this type is an array, false otherwise
     */
    boolean isArray();

    /**
     * Determines whether this type definition represents a primitive type.
     *
     * @return true if this type is a primitive, false otherwise
     */
    boolean isPrimitive();

    /**
     * Determines whether this type definition represents an interface.
     *
     * @return true if this type is an interface, false otherwise
     */
    boolean isInterface();

    /**
     * Determines whether this type definition represents an enum.
     *
     * @return true if this type is an enum, false otherwise
     */
    boolean isEnum();

    /**
     * Determines whether this type definition represents an annotation.
     *
     * @return true if this type is an annotation, false otherwise
     */
    boolean isAnnotation();

    /**
     * Returns the erasure of this type, which is the type without any generic type information.
     *
     * @return a type descriptor representing the erasure of this type
     */
    TypeDesc asErasure();

    /**
     * Returns the superclass of this type, if it's a class. Returns null if this type is an interface
     * or if it does not have a superclass (e.g., {@code java.lang.Object} or an array type).
     *
     * @return a generic type descriptor representing the superclass, or null if there is none
     */
    TypeDesc.Generic getSuperClass();

    /**
     * Returns a list of interfaces that this type directly extends or implements, if any.
     *
     * @return a list of generic type descriptors representing the interfaces
     */
    List<TypeDesc.Generic> getInterfaces();

    /**
     * If this type is an array, returns the component type of the array.
     * Otherwise, returns this type itself.
     *
     * @return the component type if this is an array type, or this type otherwise
     */
    TypeDef getComponentType();

    /**
     * Determines whether an instance of the given class can be assigned to this type.
     * This method is a convenience that delegates to the erasure of this type.
     *
     * @param type the class to check for assignability
     * @return true if the given class can be assigned to this type, false otherwise
     */
    default boolean isAssignableFrom(Class<?> type) {
        return asErasure().isAssignableFrom(type);
    }

    /**
     * Determines whether this type can be assigned to an instance of the given class.
     * This method is a convenience that delegates to the erasure of this type.
     *
     * @param type the class to check for assignability
     * @return true if this type can be assigned to the given class, false otherwise
     */
    default boolean isAssignableTo(Class<?> type) {
        return asErasure().isAssignableTo(type);
    }
}

