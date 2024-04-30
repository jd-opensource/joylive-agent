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
     * Retrieves the simple name of the type as represented by this type description.
     * For instance, in the case of a generic type parameter, this method would return
     * the name of the type parameter.
     *
     * @return a String representing the simple name of the type
     */
    String getSimpleName();

    /**
     * Obtains the component type of this type description if it represents an array type.
     * For generic types, this method returns the type that the generic type parameterizes.
     * For example, for a parameterized type such as {@code List<String>}, this method would return
     * a {@code TypeDesc} representing the {@code String} type.
     *
     * @return a {@code TypeDesc} representing the component type of the array or the parameterized type
     */
    TypeDesc getComponentType();

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

