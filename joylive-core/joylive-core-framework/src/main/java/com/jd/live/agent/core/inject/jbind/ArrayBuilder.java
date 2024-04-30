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

import com.jd.live.agent.core.extension.annotation.Extensible;

/**
 * The ArrayBuilder interface provides a contract for building instances of the ArrayObject
 * class. Implementations of this interface are capable of creating new array objects
 * either by specifying a size or by copying elements from an existing array.
 */
@Extensible("ArrayBuilder")
public interface ArrayBuilder {

    /**
     * Creates a new ArrayObject with the specified size.
     *
     * @param size the size of the new array object
     * @return a new ArrayObject of the given size
     */
    ArrayObject create(int size);

    /**
     * Creates a new ArrayObject as a copy of the specified array.
     *
     * @param array the array to copy into the new ArrayObject
     * @return a new ArrayObject that is a copy of the specified array
     */
    ArrayObject create(Object array);

    /**
     * Returns the {@code Class} representing the component type of the array. For example,
     * for an array of strings, the component type would be {@code String.class}.
     *
     * @return the {@code Class} representing the component type of the array
     */
    Class<?> getComponentType();
}
