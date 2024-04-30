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
package com.jd.live.agent.core.util.type.generic;

import java.lang.reflect.Constructor;

/**
 * Represents a generic constructor. This class extends {@link GenericExecutable} to encapsulate
 * generic information about a constructor.
 */
public class GenericConstructor extends GenericExecutable<Constructor<?>> {

    /**
     * Constructs a {@code GenericConstructor} instance.
     *
     * @param constructor The constructor that this {@code GenericConstructor} represents.
     * @param parameters  The generic parameters of the constructor. These are the types used in the
     *                    declaration of the constructor.
     * @param exceptions  The generic exceptions that the constructor can throw. These are the exception
     *                    types declared by the constructor.
     */
    public GenericConstructor(final Constructor<?> constructor, final Generic[] parameters, final Generic[] exceptions) {
        super(constructor, parameters, exceptions);
    }
}