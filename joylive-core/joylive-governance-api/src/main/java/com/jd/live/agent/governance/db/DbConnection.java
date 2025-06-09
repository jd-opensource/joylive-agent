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
package com.jd.live.agent.governance.db;

import com.jd.live.agent.core.util.option.Converts;
import com.jd.live.agent.governance.util.network.ClusterRedirect;

/**
 * A database connection that supports auto-closing and provides cluster addressing.
 */
public interface DbConnection extends AutoCloseable {
    /**
     * Default time offset (in milliseconds) for MQ message seeking.
     * Configurable via environment variable {@code MQ_SEEK_TIME_OFFSET}, defaults to 5 minutes.
     */
    long MQ_SEEK_TIME_OFFSET = Converts.getLong(System.getenv("MQ_SEEK_TIME_OFFSET"), 60 * 1000L);

    /**
     * Gets the cluster address redirection information.
     *
     * @return cluster redirect configuration
     */
    ClusterRedirect getAddress();
}