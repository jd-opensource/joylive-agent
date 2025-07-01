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
package com.jd.live.agent.plugin.failover.jedis.v3.connection;

import com.jd.live.agent.governance.db.DbAddress;
import com.jd.live.agent.governance.db.DbFailover;
import com.jd.live.agent.governance.db.DbFailoverResponse;
import redis.clients.jedis.JedisCluster;

import java.util.function.Consumer;

public class JedisClusterConnection implements JedisConnection {

    private final JedisCluster cluster;

    private final Consumer<DbAddress> onFailover;

    private volatile DbFailover failover;

    private volatile boolean closed;

    public JedisClusterConnection(JedisCluster cluster, DbFailover failover, Consumer<DbAddress> onFailover) {
        this.cluster = cluster;
        this.failover = failover;
        this.onFailover = onFailover;
    }

    @Override
    public DbFailover getFailover() {
        return failover;
    }

    @Override
    public synchronized void close() {
        if (!closed) {
            closed = true;
            cluster.close();
        }
    }

    @Override
    public boolean isClosed() {
        return closed;
    }

    @Override
    public synchronized DbFailoverResponse failover(DbAddress newAddress) {
        failover = failover.newAddress(newAddress);
        onFailover.accept(newAddress);
        return DbFailoverResponse.SUCCESS;
    }
}
