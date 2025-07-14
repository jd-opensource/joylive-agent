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
package com.jd.live.agent.plugin.failover.redisson.v3.config;

import com.jd.live.agent.plugin.failover.redisson.v3.config.AddressResolver.*;
import org.redisson.config.*;

/**
 * Extends Redisson's Config to provide unified address resolution for different server configurations.
 * Supports single server, sentinel, cluster, master/slave, and replicated setups.
 */
public class RedissonConfig extends Config {

    private final AddressResolver addressResolver;

    public RedissonConfig(Config config) {
        super(config);
        SingleServerConfig singleServerConfig = getSingleServerConfig();
        SentinelServersConfig sentinelServersConfig = getSentinelServersConfig();
        ClusterServersConfig clusterServersConfig = getClusterServersConfig();
        MasterSlaveServersConfig masterSlaveServersConfig = getMasterSlaveServersConfig();
        ReplicatedServersConfig replicatedServersConfig = getReplicatedServersConfig();
        if (singleServerConfig != null) {
            addressResolver = new SingleServerConfigResolver(singleServerConfig);
        } else if (sentinelServersConfig != null) {
            addressResolver = new SentinelServersConfigResolver(sentinelServersConfig);
        } else if (clusterServersConfig != null) {
            addressResolver = new ClusterServersConfigResolver(clusterServersConfig);
        } else if (masterSlaveServersConfig != null) {
            addressResolver = new MasterSlaveServersConfigResolver(masterSlaveServersConfig);
        } else if (replicatedServersConfig != null) {
            addressResolver = new ReplicatedServersConfigResolver(replicatedServersConfig);
        } else {
            addressResolver = null;
        }
    }

    /**
     * Gets all server addresses as array
     *
     * @return array of server addresses or null if no resolver available
     */
    public String[] getAddress() {
        return addressResolver == null ? null : addressResolver.getAddress();
    }

    /**
     * Sets server addresses
     *
     * @param address array of server addresses
     */
    public void setAddress(String[] address) {
        if (addressResolver != null) {
            addressResolver.setAddress(address);
        }
    }

    public boolean isSingleAddress() {
        return addressResolver instanceof SingleServerConfigResolver;
    }
}
