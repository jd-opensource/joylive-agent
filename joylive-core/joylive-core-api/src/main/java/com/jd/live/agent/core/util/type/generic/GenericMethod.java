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

import java.lang.reflect.Method;

/**
 * Represents a method with generic type information.
 * It extends {@code GenericExecutable<Method>} to include generic type details specific to methods.
 */
public class GenericMethod extends GenericExecutable<Method> {

    /**
     * The generic return type of the method.
     */
    protected final Generic result;

    /**
     * Constructs an instance of {@code GenericMethod} with detailed generic type information.
     *
     * @param method     The method for which the generic type information is provided.
     * @param parameters An array of {@code Generic} representing the generic parameters of the method.
     * @param exceptions An array of {@code Generic} representing the generic exceptions declared by the method.
     * @param result     A {@code Generic} representing the generic return type of the method.
     */
    public GenericMethod(final Method method,
                         final Generic[] parameters,
                         final Generic[] exceptions,
                         final Generic result) {
        super(method, parameters, exceptions);
        this.result = result;
    }

    /**
     * Retrieves the generic return type of the method.
     *
     * @return The {@code Generic} representing the return type of the method.
     */
    public Generic getResult() {
        return result;
    }

}

