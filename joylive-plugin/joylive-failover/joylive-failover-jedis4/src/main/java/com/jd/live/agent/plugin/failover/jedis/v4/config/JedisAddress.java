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
package com.jd.live.agent.plugin.failover.jedis.v4.config;

import com.jd.live.agent.core.util.network.Address;
import com.jd.live.agent.governance.db.DbAddress;
import com.jd.live.agent.governance.db.DbFailover;
import lombok.Getter;
import redis.clients.jedis.HostAndPort;

import java.util.HashSet;
import java.util.Set;

public class JedisAddress extends HostAndPort {

    @Getter
    private final DbFailover failover;

    public JedisAddress(String host, int port, DbFailover failover) {
        super(host, port);
        this.failover = failover;
    }

    public static JedisAddress of(DbFailover failover) {
        Address addr = Address.parse(failover.getNewAddress().getAddress());
        return new JedisAddress(addr.getHost(), addr.getPort(), failover);
    }

    public JedisAddress newAddress(DbAddress newAddress) {
        return of(failover.newAddress(newAddress));
    }

    public static Set<HostAndPort> getNodes(DbAddress address) {
        Set<HostAndPort> result = new HashSet<>();
        for (String node : address.getNodes()) {
            Address addr = Address.parse(node);
            result.add(new HostAndPort(addr.getHost(), addr.getPort()));
        }
        return result;
    }

    public static String getFailover(HostAndPort hp) {
        return Address.parse(hp.getHost(), hp.getPort()).toString();
    }
}
