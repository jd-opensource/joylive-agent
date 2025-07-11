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

import org.redisson.config.*;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

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
        return isSingleConfig();
    }

    /**
     * Internal interface for address resolution
     */
    private interface AddressResolver {

        /**
         * Gets all server addresses as array
         *
         * @return array of server addresses or null if no resolver available
         */
        String[] getAddress();

        /**
         * Sets server addresses
         *
         * @param address array of server addresses
         */
        void setAddress(String[] address);

    }

    /**
     * Address resolver for single server configuration
     */
    private static class SingleServerConfigResolver implements AddressResolver {

        private final SingleServerConfig singleServerConfig;

        public SingleServerConfigResolver(SingleServerConfig singleServerConfig) {
            this.singleServerConfig = singleServerConfig;
        }

        @Override
        public String[] getAddress() {
            return new String[]{singleServerConfig.getAddress()};
        }

        @Override
        public void setAddress(String[] address) {
            singleServerConfig.setAddress(address[0]);
        }
    }

    /**
     * Address resolver for sentinel configuration
     */
    private static class SentinelServersConfigResolver implements AddressResolver {
        private final SentinelServersConfig sentinelServersConfig;

        public SentinelServersConfigResolver(SentinelServersConfig sentinelServersConfig) {
            this.sentinelServersConfig = sentinelServersConfig;
        }

        @Override
        public String[] getAddress() {
            return sentinelServersConfig.getSentinelAddresses().toArray(new String[0]);
        }

        @Override
        public void setAddress(String[] address) {
            sentinelServersConfig.setSentinelAddresses(Arrays.asList(address));
        }
    }

    /**
     * Address resolver for cluster configuration
     */
    private static class ClusterServersConfigResolver implements AddressResolver {
        private final ClusterServersConfig clusterServersConfig;

        public ClusterServersConfigResolver(ClusterServersConfig clusterServersConfig) {
            this.clusterServersConfig = clusterServersConfig;
        }

        @Override
        public String[] getAddress() {
            return clusterServersConfig.getNodeAddresses().toArray(new String[0]);
        }

        @Override
        public void setAddress(String[] address) {
            clusterServersConfig.setNodeAddresses(Arrays.asList(address));
        }
    }

    /**
     * Address resolver for master/slave configuration
     */
    private static class MasterSlaveServersConfigResolver implements AddressResolver {
        private final MasterSlaveServersConfig masterSlaveServersConfig;

        public MasterSlaveServersConfigResolver(MasterSlaveServersConfig masterSlaveServersConfig) {
            this.masterSlaveServersConfig = masterSlaveServersConfig;
        }

        @Override
        public String[] getAddress() {
            Set<String> addresses = new HashSet<>();
            addresses.add(masterSlaveServersConfig.getMasterAddress());
            addresses.addAll(masterSlaveServersConfig.getSlaveAddresses());
            return addresses.toArray(new String[0]);
        }

        @Override
        public void setAddress(String[] address) {
            masterSlaveServersConfig.setMasterAddress(address[0]);
            Set<String> addresses = new HashSet<>();
            for (int i = 1; i < address.length; i++) {
                addresses.add(address[i]);
            }
            masterSlaveServersConfig.setSlaveAddresses(addresses);
        }
    }

    /**
     * Address resolver for replicated servers configuration
     */
    private static class ReplicatedServersConfigResolver implements AddressResolver {

        private final ReplicatedServersConfig replicatedServersConfig;

        public ReplicatedServersConfigResolver(ReplicatedServersConfig replicatedServersConfig) {
            this.replicatedServersConfig = replicatedServersConfig;
        }

        @Override
        public String[] getAddress() {
            return replicatedServersConfig.getNodeAddresses().toArray(new String[0]);
        }

        @Override
        public void setAddress(String[] address) {
            replicatedServersConfig.setNodeAddresses(Arrays.asList(address));
        }
    }
}
