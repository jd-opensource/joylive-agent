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
package com.jd.live.agent.plugin.failover.jedis.v6.connection;

import com.jd.live.agent.governance.db.DbAddress;
import com.jd.live.agent.governance.db.DbConnection;
import com.jd.live.agent.governance.db.DbFailover;
import com.jd.live.agent.governance.db.DbFailoverResponse;
import org.springframework.data.redis.connection.RedisConnection;

import java.util.function.Consumer;
import java.util.function.Function;

public class JedisUnpoolSpringConnection extends JedisUnpoolConnection implements JedisConnection {

    private final Function<DbAddress, JedisUnpoolConnection> creator;

    private final Consumer<DbConnection> closer;

    public JedisUnpoolSpringConnection(RedisConnection delegate,
                                       DbFailover failover,
                                       Consumer<DbConnection> closer,
                                       Function<DbAddress, JedisUnpoolConnection> creator
    ) {
        super(delegate, failover);
        this.closer = closer;
        this.creator = creator;
    }

    @Override
    protected void doClose() {
        super.doClose();
        if (closer != null) {
            closer.accept(this);
        }
    }

    @Override
    public DbFailover getFailover() {
        return failover;
    }

    @Override
    public synchronized DbFailoverResponse failover(DbAddress newAddress) {
        try {
            // recreate jedis
            JedisUnpoolConnection newConn = creator.apply(newAddress);
            // user failover new address.
            this.failover = newConn.getFailover();
            return DbFailoverResponse.SUCCESS;
        } catch (Throwable e) {
            // retry on failed
            return DbFailoverResponse.FAILED;
        }
    }
}
