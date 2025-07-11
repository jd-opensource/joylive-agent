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

import com.jd.live.agent.core.util.URI;
import com.jd.live.agent.core.util.network.Address;
import org.redisson.config.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import static com.jd.live.agent.core.util.CollectionUtils.toList;

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
         * @param addresses array of server addresses
         */
        void setAddress(String[] addresses);

        static Address getAddress(String address) {
            return Address.parse(address, true, 6379);
        }

    }

    /**
     * Address resolver for single server configuration
     */
    private static class SingleServerConfigResolver implements AddressResolver {

        private final SingleServerConfig singleServerConfig;

        private URI uri;

        SingleServerConfigResolver(SingleServerConfig singleServerConfig) {
            this.singleServerConfig = singleServerConfig;
            this.uri = URI.parse(singleServerConfig.getAddress());
        }

        @Override
        public String[] getAddress() {
            return new String[]{uri.getAddress()};
        }

        @Override
        public void setAddress(String[] addresses) {
            Address addr = AddressResolver.getAddress(addresses[0]);
            uri = uri.address(addr.getHost(), addr.getPort());
            singleServerConfig.setAddress(uri.getAddress(true));
        }
    }

    /**
     * Address resolver for sentinel configuration
     */
    private static abstract class MultiAddressResolver<T> implements AddressResolver {

        protected T config;

        private List<URI> uris;

        MultiAddressResolver(T config) {
            this.config = config;
            this.uris = toList(getAddress(config), URI::parse);
        }

        @Override
        public String[] getAddress() {
            return toList(uris, URI::getAddress).toArray(new String[0]);
        }

        @Override
        public void setAddress(String[] addresses) {
            URI uri = uris.get(0);
            uris = toList(toList(Arrays.asList(addresses), AddressResolver::getAddress), addr -> uri.address(addr.getHost(), addr.getPort()));
            setAddress(config, toList(uris, u -> u.getAddress(true)));
        }

        protected abstract List<String> getAddress(T config);

        protected abstract void setAddress(T config, List<String> addresses);
    }

    /**
     * Address resolver for sentinel configuration
     */
    private static class SentinelServersConfigResolver extends MultiAddressResolver<SentinelServersConfig> {

        SentinelServersConfigResolver(SentinelServersConfig sentinelServersConfig) {
            super(sentinelServersConfig);
        }

        @Override
        protected List<String> getAddress(SentinelServersConfig config) {
            return config.getSentinelAddresses();
        }

        @Override
        protected void setAddress(SentinelServersConfig config, List<String> addresses) {
            config.setSentinelAddresses(addresses);
        }
    }

    /**
     * Address resolver for cluster configuration
     */
    private static class ClusterServersConfigResolver extends MultiAddressResolver<ClusterServersConfig> {

        ClusterServersConfigResolver(ClusterServersConfig clusterServersConfig) {
            super(clusterServersConfig);
        }

        @Override
        protected List<String> getAddress(ClusterServersConfig config) {
            return config.getNodeAddresses();
        }

        @Override
        protected void setAddress(ClusterServersConfig config, List<String> addresses) {
            config.setNodeAddresses(addresses);
        }
    }

    /**
     * Address resolver for master/slave configuration
     */
    private static class MasterSlaveServersConfigResolver extends MultiAddressResolver<MasterSlaveServersConfig> {

        MasterSlaveServersConfigResolver(MasterSlaveServersConfig config) {
            super(config);
        }

        @Override
        protected List<String> getAddress(MasterSlaveServersConfig config) {
            List<String> result = new ArrayList<>();
            result.add(config.getMasterAddress());
            result.addAll(config.getSlaveAddresses());
            return result;
        }

        @Override
        protected void setAddress(MasterSlaveServersConfig config, List<String> addresses) {
            config.setMasterAddress(addresses.get(0));
            config.setSlaveAddresses(new HashSet<>(addresses.subList(1, addresses.size())));
        }
    }

    /**
     * Address resolver for replicated servers configuration
     */
    private static class ReplicatedServersConfigResolver extends MultiAddressResolver<ReplicatedServersConfig> {

        ReplicatedServersConfigResolver(ReplicatedServersConfig clusterServersConfig) {
            super(clusterServersConfig);
        }

        @Override
        protected List<String> getAddress(ReplicatedServersConfig config) {
            return config.getNodeAddresses();
        }

        @Override
        protected void setAddress(ReplicatedServersConfig config, List<String> addresses) {
            config.setNodeAddresses(addresses);
        }
    }
}
