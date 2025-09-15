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
package com.jd.live.agent.implement.bytekit.bytebuddy.util;

import net.bytebuddy.agent.builder.AgentBuilder.PoolStrategy;

/**
 * Utility class for managing ByteBuddy TypePool instances with caching support.
 * Provides centralized access to cached TypePools and PoolStrategy configurations.
 */
public class PoolUtil {

    private static PoolCache typePoolCache = new PoolCache(256);

    private static PoolStrategy poolStrategy = new PoolStrategy.WithTypePoolCache.Simple(typePoolCache);

    /**
     * Returns the global TypePool cache instance.
     *
     * @return the shared PoolCache instance
     */
    public static PoolCache getTypePoolCache() {
        return typePoolCache;
    }

    /**
     * Returns the configured PoolStrategy with caching enabled.
     *
     * @return the cached PoolStrategy instance
     */
    public static PoolStrategy getPoolStrategy() {
        return poolStrategy;
    }

}
