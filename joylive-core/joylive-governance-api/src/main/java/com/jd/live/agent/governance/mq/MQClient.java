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
package com.jd.live.agent.governance.mq;

import com.jd.live.agent.governance.db.DbConnection;
import com.jd.live.agent.governance.util.network.ClusterAddress;

/**
 * Core message queue client interface providing lifecycle operations.
 */
public interface MQClient extends MQClientConfig, DbConnection {

    /**
     * Gets the client's role (producer/consumer).
     */
    MQClientRole getRole();

    /**
     * Performs implementation-specific resource cleanup.
     */
    void close();

    /**
     * Performs implementation-specific initialization.
     *
     * @throws Exception if client fails to start
     */
    void start() throws Exception;

    /**
     * Reconnects to a new cluster address and resets consumption offsets.
     *
     * @param newAddress the new cluster address to connect to
     */
    void reconnect(ClusterAddress newAddress);
}
