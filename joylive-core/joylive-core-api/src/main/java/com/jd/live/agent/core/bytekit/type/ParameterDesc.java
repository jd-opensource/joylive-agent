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
 * The {@code ParameterDesc} interface represents a description of a parameter within a
 * method or constructor. It provides methods to access various attributes of the parameter,
 * such as its type, name, index, and the method it belongs to. Additionally, it includes
 * functionality to retrieve annotations associated with the parameter.
 *
 * @since 1.0.0
 */
public interface ParameterDesc extends AnnotationSource, NamedElement {

    /**
     * Gets the zero-based index of the parameter in the method or constructor's parameter list.
     * For example, the first parameter has an index of 0, the second parameter has an index of 1, and so on.
     *
     * @return an int representing the index of the parameter
     */
    int getIndex();

    /**
     * Retrieves the method description ({@code MethodDesc}) that declares this parameter.
     * This can be used to obtain further details about the method, such as its name,
     * return type, and the list of all parameters it defines.
     *
     * @return a {@code MethodDesc} object representing the declaring method
     */
    MethodDesc getDeclaringMethod();

    /**
     * Gets the generic type description of the parameter. If the parameter is not generic,
     * this method returns a type description representing the raw type.
     * For example, for a parameter of type {@code List<String>}, this method would return
     * a {@code TypeDesc.Generic} representing the {@code List} type with {@code String} as its parameterized type.
     *
     * @return a {@code TypeDesc.Generic} representing the generic type of the parameter
     */
    TypeDesc.Generic getType();
}
