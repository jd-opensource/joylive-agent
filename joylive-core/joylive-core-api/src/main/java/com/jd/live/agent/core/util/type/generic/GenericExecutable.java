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

import lombok.Getter;

import java.lang.reflect.Executable;
import java.lang.reflect.Type;

/**
 * Abstract class representing a generic executable element such as a method or constructor.
 * It provides a structure for capturing and providing generic type information.
 *
 * @param <T> The type of the executable, which must be a subclass of {@link Executable}.
 */
@Getter
public abstract class GenericExecutable<T extends Executable> {

    /**
     * The executable element, such as a method or constructor.
     */
    protected T method;

    /**
     * An array of {@code Generic} objects representing the generic parameters of the executable.
     */
    protected Generic[] parameters;

    /**
     * An array of {@code Generic} objects representing the generic exceptions declared by the executable.
     */
    protected Generic[] exceptions;

    /**
     * An array of {@code Type} objects representing the resolved types of the generic parameters.
     */
    protected Type[] types;

    /**
     * An array of {@code Class<?>} objects representing the raw types (erasure) of the generic parameters.
     */
    protected Class<?>[] erasures;

    /**
     * Constructor for creating a {@code GenericExecutable} instance with the specified executable,
     * its generic parameters, and its generic exceptions.
     *
     * @param method     The executable element.
     * @param parameters An array of {@code Generic} objects representing the generic parameters.
     * @param exceptions An array of {@code Generic} objects representing the generic exceptions.
     */
    public GenericExecutable(final T method, final Generic[] parameters, final Generic[] exceptions) {
        this.method = method;
        this.parameters = parameters;
        this.exceptions = exceptions;
        this.types = new Type[parameters.length];
        this.erasures = new Class<?>[parameters.length];
        // Initialize the types and erasures arrays based on the provided generic parameters.
        for (int i = 0; i < parameters.length; i++) {
            types[i] = parameters[i].getType();
            erasures[i] = parameters[i].getErasure();
        }
    }

}
