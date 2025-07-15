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
package com.jd.live.agent.plugin.failover.redisson.v3.connection;

import com.jd.live.agent.governance.db.DbAddress;
import com.jd.live.agent.governance.db.DbConnection;
import com.jd.live.agent.governance.db.DbFailover;
import com.jd.live.agent.governance.db.DbFailoverResponse;
import lombok.Getter;
import org.redisson.Redisson;

import java.util.function.Consumer;

public class RedissonConnection implements DbConnection {

    @Getter
    private final Redisson redisson;

    private final Consumer<DbAddress> onFailover;

    @Getter
    private volatile DbFailover failover;

    public RedissonConnection(Redisson redisson, DbFailover failover, Consumer<DbAddress> onFailover) {
        this.redisson = redisson;
        this.failover = failover;
        this.onFailover = onFailover;
    }

    @Override
    public void close() {
        redisson.shutdown();
    }

    @Override
    public DbFailoverResponse failover(DbAddress newAddress) {
        DbFailover newFailover = failover.newAddress(newAddress);
        this.failover = newFailover;
        onFailover.accept(newAddress);
        return DbFailoverResponse.SUCCESS;
    }
}
