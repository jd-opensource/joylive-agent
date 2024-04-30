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

import java.util.List;

/**
 * The {@code MethodSource} interface provides a mechanism to retrieve method descriptions
 * from an underlying element, typically a class or an object. This interface is useful for
 * reflective operations where method information is required without invoking the methods.
 *
 * @author Zhiguo.Chen
 * @since 1.0.0
 */
public interface MethodSource {

    /**
     * Returns a list of method descriptions representing all the methods declared by the
     * underlying element that this interface is associated with. This includes public,
     * protected, default (package) access, and private methods, but excludes methods from
     * superclasses or interfaces.
     *
     * @return a List of {@link MethodDesc} objects describing the declared methods
     */
    List<MethodDesc> getDeclaredMethods();
}

