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
 * The {@code MethodDesc} interface represents a method or constructor within a class. It
 * provides information about the method's signature, annotations, modifiers, and name, as
 * well as the ability to determine if it is a constructor, a regular method, or a default method.
 *
 * @author Zhiguo.Chen
 * @since 1.0.0
 */
public interface MethodDesc extends AnnotationSource, ModifierDesc, NamedElement, ParameterSource {

    /**
     * Determines whether this method descriptor represents a constructor.
     *
     * @return {@code true} if this is a constructor; {@code false} otherwise
     */
    boolean isConstructor();

    /**
     * Determines whether this method descriptor represents a regular method.
     *
     * @return {@code true} if this is a regular method; {@code false} otherwise
     */
    boolean isMethod();

    /**
     * Determines whether this method descriptor represents a default method, which is a method
     * that is declared in an interface with an implementation.
     *
     * @return {@code true} if this is a default method; {@code false} otherwise
     */
    boolean isDefaultMethod();

    /**
     * Provides a human-readable description of this method, which typically includes the method
     * name, return type, and parameter list.
     *
     * @return a String describing the method
     */
    String getDescription();
}
