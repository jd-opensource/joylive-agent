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

import com.jd.live.agent.core.exception.PluginException;
import com.jd.live.agent.core.extension.Name;

/**
 * A concrete implementation of the {@link Instantiation} interface that uses the {@code newInstance()}
 * method from the {@code Class} class to create new instances.
 */
public class ClassInstantiation implements Instantiation {

    /**
     * The singleton instance of the {@code ClassInstantiation}.
     */
    public static final Instantiation INSTANCE = new ClassInstantiation();

    /**
     * Creates a new instance of the specified type using the class's {@code newInstance()} method.
     *
     * @param name The {@code Name} object containing the class from which to create an instance.
     * @param <T>  The type of the instance to be created.
     * @return A new instance of type T.
     * @throws PluginException If there is an error during instantiation.
     */
    @Override
    public <T> T newInstance(final Name<T> name) {
        try {
            return name == null ? null : name.getClazz().newInstance();
        } catch (Throwable e) {
            throw new PluginException("an error occurred while instancing class. " + name.getClazz().getName(), e);
        }
    }
}
