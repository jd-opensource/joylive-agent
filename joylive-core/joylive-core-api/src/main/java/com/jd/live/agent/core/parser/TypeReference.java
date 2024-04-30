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
package com.jd.live.agent.core.parser;

import lombok.Getter;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * A utility class to capture and pass generic type information.
 * <p>
 * This abstract class is meant to be subclassed in order to capture the generic type information
 * T by creating an anonymous inner class. This technique allows the actual type arguments to be
 * preserved and retrieved at runtime despite Java's type erasure.
 */
@Getter
public abstract class TypeReference<T> {

    /**
     * The type captured from the generic parameter. It represents the actual type argument
     * to the generic class T.
     */
    protected final Type type;

    /**
     * Constructor that initializes {@code type} by using reflection to inspect the actual type
     * arguments provided to the generic superclass. This approach retrieves the specific
     * generic type information lost due to type erasure.
     */
    protected TypeReference() {
        this.type = ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0];
    }

}

