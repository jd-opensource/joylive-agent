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
package com.jd.live.agent.plugin.failover.jedis.v5.config;

import com.jd.live.agent.core.util.network.Address;
import com.jd.live.agent.governance.util.network.ClusterAddress;
import com.jd.live.agent.governance.util.network.ClusterRedirect;
import lombok.Getter;
import redis.clients.jedis.HostAndPort;

public class JedisAddress extends HostAndPort {

    @Getter
    private final ClusterRedirect address;

    public JedisAddress(String host, int port, ClusterRedirect address) {
        super(host, port);
        this.address = address;
    }

    public static JedisAddress of(ClusterRedirect address) {
        Address addr = Address.parse(address.getNewAddress().getAddress());
        return new JedisAddress(addr.getHost(), addr.getPort(), address);
    }

    public JedisAddress newAddress(ClusterAddress newAddress) {
        return of(address.newAddress(newAddress));
    }
}
