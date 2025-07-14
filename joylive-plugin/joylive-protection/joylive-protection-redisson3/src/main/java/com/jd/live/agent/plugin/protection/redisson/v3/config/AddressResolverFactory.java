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

import com.jd.live.agent.bootstrap.util.type.FieldAccessor;
import com.jd.live.agent.bootstrap.util.type.FieldAccessorFactory;
import org.redisson.config.ClusterServersConfig;
import org.redisson.config.MasterSlaveServersConfig;
import org.redisson.config.ReplicatedServersConfig;
import org.redisson.config.SentinelServersConfig;
import org.redisson.connection.*;

/**
 * Factory for creating address resolvers based on connection manager type.
 * Uses reflection to access connection manager configuration.
 */
public class AddressResolverFactory {

    private static final FieldAccessor configAccessor = FieldAccessorFactory.getAccessor(MasterSlaveConnectionManager.class, "config");

    /**
     * Creates an appropriate address resolver for the given connection manager.
     *
     * @param connectionManager the connection manager instance
     * @return configured address resolver, or null if type is unsupported
     */
    public static AddressResolver getResolver(ConnectionManager connectionManager) {
        AddressResolver resolver = null;
        if (connectionManager instanceof ClusterConnectionManager) {
            resolver = new AddressResolver.ClusterServersAddressResolver((ClusterServersConfig) configAccessor.get(connectionManager));
        } else if (connectionManager instanceof SentinelConnectionManager) {
            resolver = new AddressResolver.SentinelServersAddressResolver((SentinelServersConfig) configAccessor.get(connectionManager));
        } else if (connectionManager instanceof ReplicatedConnectionManager) {
            resolver = new AddressResolver.ReplicatedServersAddressResolver((ReplicatedServersConfig) configAccessor.get(connectionManager));
        } else if (connectionManager instanceof SingleConnectionManager) {
            resolver = new AddressResolver.SingleServerAddressResolver((MasterSlaveServersConfig) configAccessor.get(connectionManager));
        } else if (connectionManager instanceof MasterSlaveConnectionManager) {
            resolver = new AddressResolver.MasterSlaveServersAddressResolver((MasterSlaveServersConfig) configAccessor.get(connectionManager));
        }
        return resolver;
    }

    /**
     * Gets server addresses from the connection manager.
     * Delegates to {@link AddressResolverFactory#getResolver(ConnectionManager)} to resolve addresses.
     *
     * @param connectionManager the connection manager to query
     * @return array of server addresses, or null if no resolver is available
     */
    public static String[] getAddresses(ConnectionManager connectionManager) {
        AddressResolver resolver = getResolver(connectionManager);
        return resolver == null ? null : resolver.getAddress();
    }
}
