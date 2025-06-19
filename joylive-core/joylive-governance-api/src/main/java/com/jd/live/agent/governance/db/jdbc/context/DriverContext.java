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
package com.jd.live.agent.governance.db.jdbc.context;

import com.jd.live.agent.governance.db.jdbc.datasource.LiveDataSource;

/**
 * Thread-local context for tracking active {@link LiveDataSource}.
 * Manages data source association for the current execution thread.
 */
public class DriverContext {

    private static final ThreadLocal<LiveDataSource> POOL_LOCAL = new ThreadLocal<>();

    /**
     * @return the LiveDataSource bound to current thread
     */
    public static LiveDataSource get() {
        return POOL_LOCAL.get();
    }

    /**
     * Binds a LiveDataSource to current thread
     *
     * @param dataSource the data source to associate
     */
    public static void set(LiveDataSource dataSource) {
        POOL_LOCAL.set(dataSource);
    }

    /**
     * Clears the thread's LiveDataSource binding
     */
    public static void remove() {
        POOL_LOCAL.remove();
    }

}
