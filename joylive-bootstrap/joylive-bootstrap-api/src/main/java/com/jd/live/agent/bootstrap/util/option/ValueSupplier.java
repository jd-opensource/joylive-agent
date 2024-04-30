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
package com.jd.live.agent.bootstrap.util.option;

/**
 * A functional interface representing a supplier of values based on a given key.
 */
@FunctionalInterface
public interface ValueSupplier {

    /**
     * Retrieves an object of type T based on the specified key.
     *
     * @param key The key to retrieve the value for.
     * @param <T> The type of the object to be returned.
     * @return The object associated with the specified key, or null if not found.
     */
    <T> T getObject(String key);

}

