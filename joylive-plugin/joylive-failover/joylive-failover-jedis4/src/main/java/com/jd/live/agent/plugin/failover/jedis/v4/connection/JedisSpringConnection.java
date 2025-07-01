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
package com.jd.live.agent.plugin.failover.jedis.v4.connection;

import com.jd.live.agent.bootstrap.util.type.UnsafeFieldAccessor;
import com.jd.live.agent.governance.db.DbAddress;
import com.jd.live.agent.governance.db.DbConnection;
import com.jd.live.agent.governance.db.DbFailover;
import com.jd.live.agent.governance.db.DbFailoverResponse;

import java.util.function.Consumer;
import java.util.function.Function;

public class JedisSpringConnection extends JedisFailoverConnection implements JedisConnection {

    private final UnsafeFieldAccessor jedisAccessor;

    private final Function<DbAddress, JedisFailoverConnection> creator;

    private final Consumer<DbConnection> closer;

    public JedisSpringConnection(org.springframework.data.redis.connection.jedis.JedisConnection delegate,
                                 DbFailover failover,
                                 UnsafeFieldAccessor jedisAccessor,
                                 Function<DbAddress, JedisFailoverConnection> creator,
                                 Consumer<DbConnection> closer) {
        super(delegate, failover);
        this.jedisAccessor = jedisAccessor;
        this.creator = creator;
        this.closer = closer;
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
            JedisFailoverConnection conn = creator.apply(newAddress);
            jedisAccessor.set(delegate, conn.getDelegate().getJedis());
            // user failover new address.
            this.failover = conn.getFailover();
            return DbFailoverResponse.SUCCESS;
        } catch (Throwable e) {
            // retry on failed
            return DbFailoverResponse.FAILED;
        }
    }
}
