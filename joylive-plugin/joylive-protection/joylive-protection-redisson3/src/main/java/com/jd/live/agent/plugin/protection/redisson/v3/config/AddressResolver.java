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
package com.jd.live.agent.plugin.protection.redisson.v3.config;

import com.jd.live.agent.core.util.URI;
import org.redisson.config.ClusterServersConfig;
import org.redisson.config.MasterSlaveServersConfig;
import org.redisson.config.ReplicatedServersConfig;
import org.redisson.config.SentinelServersConfig;

import java.util.ArrayList;
import java.util.List;

import static com.jd.live.agent.core.util.CollectionUtils.toList;

/**
 * Internal interface for address resolution
 */
public interface AddressResolver {

    /**
     * Gets all server addresses as array
     *
     * @return array of server addresses or null if no resolver available
     */
    String[] getAddress();

    /**
     * Address resolver for single server configuration
     */
    class SingleServerAddressResolver implements AddressResolver {

        private final MasterSlaveServersConfig config;

        private final URI uri;

        public SingleServerAddressResolver(MasterSlaveServersConfig config) {
            this.config = config;
            this.uri = URI.parse(config.getMasterAddress());
        }

        @Override
        public String[] getAddress() {
            return new String[]{uri.getAddress()};
        }

    }

    /**
     * Address resolver for sentinel configuration
     */
    abstract class MultiAddressResolver<T> implements AddressResolver {

        protected T config;

        private final List<URI> uris;

        public MultiAddressResolver(T config) {
            this.config = config;
            this.uris = toList(getAddress(config), URI::parse);
        }

        @Override
        public String[] getAddress() {
            return toList(uris, URI::getAddress).toArray(new String[0]);
        }

        protected abstract List<String> getAddress(T config);

    }

    /**
     * Address resolver for sentinel configuration
     */
    class SentinelServersAddressResolver extends MultiAddressResolver<SentinelServersConfig> {

        public SentinelServersAddressResolver(SentinelServersConfig sentinelServersConfig) {
            super(sentinelServersConfig);
        }

        @Override
        protected List<String> getAddress(SentinelServersConfig config) {
            return config.getSentinelAddresses();
        }

    }

    /**
     * Address resolver for cluster configuration
     */
    class ClusterServersAddressResolver extends MultiAddressResolver<ClusterServersConfig> {

        public ClusterServersAddressResolver(ClusterServersConfig config) {
            super(config);
        }

        @Override
        protected List<String> getAddress(ClusterServersConfig config) {
            return config.getNodeAddresses();
        }

    }

    /**
     * Address resolver for master/slave configuration
     */
    class MasterSlaveServersAddressResolver extends MultiAddressResolver<MasterSlaveServersConfig> {

        public MasterSlaveServersAddressResolver(MasterSlaveServersConfig config) {
            super(config);
        }

        @Override
        protected List<String> getAddress(MasterSlaveServersConfig config) {
            List<String> result = new ArrayList<>();
            result.add(config.getMasterAddress());
            result.addAll(config.getSlaveAddresses());
            return result;
        }
    }

    /**
     * Address resolver for replicated servers configuration
     */
    class ReplicatedServersAddressResolver extends MultiAddressResolver<ReplicatedServersConfig> {

        public ReplicatedServersAddressResolver(ReplicatedServersConfig config) {
            super(config);
        }

        @Override
        protected List<String> getAddress(ReplicatedServersConfig config) {
            return config.getNodeAddresses();
        }

    }
}
