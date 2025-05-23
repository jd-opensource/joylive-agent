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

import lombok.Getter;

/**
 * Immutable snapshot of an object pool's state.
 * <p>
 * Provides counts of available, in-use, and maximum capacity.
 */
@Getter
public class PoolStatus {
    /**
     * Number of objects currently available in pool
     */
    private final int pooledObjects;

    /**
     * Number of objects currently checked out
     */
    private final int activeObjects;

    /**
     * Maximum capacity of the pool
     */
    private final int maxCapacity;

    /**
     * Creates a new status snapshot
     *
     * @param pooled available object count (must be ≥ 0)
     * @param active in-use object count (must be ≥ 0)
     * @param max    maximum pool capacity (must be ≥ 1)
     */
    public PoolStatus(int pooled, int active, int max) {
        this.pooledObjects = pooled;
        this.activeObjects = active;
        this.maxCapacity = max;
    }
}
