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
 * The {@code ModifierDesc} interface provides access to the modifiers of a Java element such as a
 * class, method, or field. Modifiers in Java are used to specify the access level and other
 * properties of these elements.
 *
 * @author Zhiguo.Chen
 * @since 1.0.0
 */
public interface ModifierDesc {
    /**
     * Returns the modifiers of the element encoded as an integer value. This integer follows
     * the conventions defined in the {@code java.lang.reflect.Modifier} class, where each modifier
     * has a unique bit that when set, represents the presence of that modifier.
     *
     * @return an int representing the modifiers of the element
     */
    int getModifiers();

    /**
     * Determines whether the element is declared as final. A final class cannot be subclassed,
     * a final method cannot be overridden, and a final variable cannot be reassigned.
     *
     * @return a boolean indicating if the element is final
     */
    boolean isFinal();

    /**
     * Determines whether the element is declared as static. A static member belongs to the class
     * rather than to any specific instance of the class.
     *
     * @return a boolean indicating if the element is static
     */
    boolean isStatic();

    /**
     * Determines whether the element is declared as public. Public elements can be accessed from
     * any other class.
     *
     * @return a boolean indicating if the element is public
     */
    boolean isPublic();

    /**
     * Determines whether the element is declared as protected. Protected elements can be accessed
     * from classes in the same package and subclasses.
     *
     * @return a boolean indicating if the element is protected
     */
    boolean isProtected();

    /**
     * Determines whether the element is declared as private. Private elements can only be accessed
     * from within the class in which they are declared.
     *
     * @return a boolean indicating if the element is private
     */
    boolean isPrivate();
}

