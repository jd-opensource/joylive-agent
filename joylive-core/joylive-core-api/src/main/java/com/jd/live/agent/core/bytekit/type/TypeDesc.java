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

/**
 * The {@code TypeDesc} interface represents a type description within a type system,
 * providing methods to inspect and manipulate type information. It extends several interfaces:
 * {@code TypeDef} for general type definition capabilities,
 * {@code ModifierDesc} for modifier-related properties,
 * and {@code AnnotationSource} for annotation-related functionalities.
 *
 * @since 1.0.0
 */
public interface TypeDesc extends TypeDef, ModifierDesc, AnnotationSource {

    /**
     * Safely gets the name of the super class by reading bytecode metadata, without triggering class loading.
     *
     * @return The fully qualified name of the super class, or {@code null} if this type is an interface or java.lang.Object.
     */
    String getSuperName();

    /**
     * Safely gets the names of all implemented interfaces by reading bytecode metadata, without triggering class loading.
     *
     * @return An array of fully qualified interface names, or an empty array if none.
     */
    String[] getInterfaceNames();

    /**
     * Gets the TypePool that this type description belongs to. This is necessary for looking up
     * other related types without triggering class loading.
     *
     * @return The associated TypePool.
     */
    TypePool getTypePool();

    /**
     * The {@code Generic} subinterface of {@code TypeDesc} represents a generic type.
     * It extends {@code TypeDef} to provide additional methods specific to generic types.
     */
    interface Generic extends TypeDef {

        /**
         * Returns the component type of this generic type. For example, if this generic type
         * represents a parameterized type such as {@code List<String>}, this method would return
         * the generic type description for the type {@code String}.
         *
         * @return a {@code Generic} representing the component type of this generic type
         */
        Generic getComponentType();
    }
}

