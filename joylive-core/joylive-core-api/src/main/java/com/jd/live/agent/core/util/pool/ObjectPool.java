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
package com.jd.live.agent.core.util.pool;

import com.jd.live.agent.core.extension.annotation.Extensible;

/**
 * Thread-safe pool of reusable objects with automatic validation.
 *
 * @param <T> the type of objects in the pool
 * @see Extensible
 */
@Extensible("ObjectPool")
public interface ObjectPool<T> {

    /**
     * Borrows an object from the pool. Creates new instances when empty.
     *
     * @return a validated object (never null)
     */
    T borrow();

    /**
     * Returns an object to the pool. Invalid objects are discarded.
     *
     * @param obj the object to return (ignored if null)
     */
    void release(T obj);

    /**
     * Gets current pool statistics.
     *
     * @return snapshot of pool status (active/available counts)
     */
    PoolStatus getStatus();
}
