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
package com.jd.live.agent.core.extension.jplug;

import com.jd.live.agent.core.extension.Name;

/**
 * A functional interface for instantiation that enables the creation of instances by name.
 */
@FunctionalInterface
public interface Instantiation {

    /**
     * Creates a new instance of the specified type based on the given name.
     *
     * @param name The name representing the type of instance to be created.
     * @param <T>  The type of the instance to be created.
     * @return A new instance of type T.
     */
    <T> T newInstance(Name<T> name);

}

