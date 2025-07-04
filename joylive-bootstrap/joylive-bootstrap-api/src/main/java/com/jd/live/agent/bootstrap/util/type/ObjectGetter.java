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
package com.jd.live.agent.bootstrap.util.type;

/**
 * An interface that defines a method for retrieving a value from a target object.
 */
@FunctionalInterface
public interface ObjectGetter {

    /**
     * Retrieves a value from the specified target object.
     *
     * @param target The target object from which the value is to be retrieved.
     * @return The value retrieved from the target object.
     */
    Object get(Object target);

    /**
     * Gets a value from the target object and casts it to the specified type.
     *
     * @param <T>    the type to cast the value to
     * @param target the object to get the value from
     * @param type   the expected class type of the return value
     * @return the casted value if the value is of the specified type, otherwise null
     */
    @SuppressWarnings("unchecked")
    default <T> T get(Object target, Class<T> type) {
        Object value = get(target);
        return type.isInstance(value) ? (T) value : null;
    }
}

