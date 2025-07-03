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
package com.jd.live.agent.plugin.protection.lettuce.v6.interceptor;

import com.jd.live.agent.bootstrap.util.type.UnsafeFieldAccessor;
import com.jd.live.agent.bootstrap.util.type.UnsafeFieldAccessorFactory;
import com.jd.live.agent.core.util.network.Address;
import com.jd.live.agent.core.util.type.ClassUtils;
import com.jd.live.agent.governance.config.GovernanceConfig;
import com.jd.live.agent.governance.config.RedisConfig;
import com.jd.live.agent.governance.interceptor.AbstractDbInterceptor;
import com.jd.live.agent.governance.policy.PolicySupplier;
import io.lettuce.core.CommandListenerWriter;
import io.lettuce.core.RedisChannelHandler;
import io.lettuce.core.RedisURI;
import io.lettuce.core.cluster.RedisClusterClient;
import io.lettuce.core.protocol.CommandExpiryWriter;
import io.lettuce.core.protocol.DefaultEndpoint;
import io.netty.channel.Channel;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.List;

import static com.jd.live.agent.core.util.CollectionUtils.toList;

/**
 * RedisChannelHandlerInterceptor
 */
public abstract class RedisChannelHandlerInterceptor extends AbstractDbInterceptor {

    protected final RedisConfig redisConfig;

    public RedisChannelHandlerInterceptor(PolicySupplier policySupplier, GovernanceConfig governanceConfig) {
        super(policySupplier);
        this.redisConfig = governanceConfig.getRedisConfig();
    }

    protected static class Accessor {

        private static final ClassLoader classloader = RedisChannelHandler.class.getClassLoader();
        private static final Class<?> masterReplicaType = ClassUtils.loadClass("io.lettuce.core.masterreplica.MasterReplicaChannelWriter", classloader);
        private static final Class<?> clusterDistributionType = ClassUtils.loadClass("io.lettuce.core.cluster.ClusterDistributionChannelWriter", classloader);
        private static final Class<?> masterReplicaConnectionProviderType = ClassUtils.loadClass("io.lettuce.core.masterreplica.MasterReplicaConnectionProvider", classloader);
        private static final Class<?> clusterPubSubConnectionProviderType = ClassUtils.loadClass("io.lettuce.core.cluster.ClusterPubSubConnectionProvider", classloader);
        private static final Class<?> pooledClusterConnectionProvider = ClassUtils.loadClass("io.lettuce.core.cluster.PooledClusterConnectionProvider", classloader);

        private static final AttributeKey<String> REDIS_URI = AttributeKey.valueOf("RedisURI");

        private static final UnsafeFieldAccessor channelWriter = UnsafeFieldAccessorFactory.getAccessor(RedisChannelHandler.class, "channelWriter");
        private static final UnsafeFieldAccessor channel = UnsafeFieldAccessorFactory.getAccessor(DefaultEndpoint.class, "channel");
        private static final UnsafeFieldAccessor masterReplicaConnectionProvider = UnsafeFieldAccessorFactory.getAccessor(masterReplicaType, "masterReplicaConnectionProvider");
        private static final UnsafeFieldAccessor initialRedisUri = UnsafeFieldAccessorFactory.getAccessor(masterReplicaConnectionProviderType, "initialRedisUri");
        private static final UnsafeFieldAccessor clusterConnectionProvider = UnsafeFieldAccessorFactory.getAccessor(clusterDistributionType, "clusterConnectionProvider");
        private static final UnsafeFieldAccessor clusterPubSubClient = UnsafeFieldAccessorFactory.getAccessor(clusterPubSubConnectionProviderType, "redisClusterClient");
        private static final UnsafeFieldAccessor pooledClusterClient = UnsafeFieldAccessorFactory.getAccessor(pooledClusterConnectionProvider, "redisClusterClient");
        private static final UnsafeFieldAccessor initialUris = UnsafeFieldAccessorFactory.getAccessor(RedisClusterClient.class, "initialUris");

        /**
         * Gets Redis server addresses from the target object.
         *
         * @param target the connection-related object to extract addresses from
         * @return array of address strings in "host:port" format, or null if not found
         */
        public static String[] getAddresses(Object target) {
            Object writer = channelWriter.get(target);
            return getAddressOfWriter(writer);
        }

        /**
         * Recursively unwraps writer delegates until finding an address source.
         *
         * @param writer the writer object to inspect
         * @return addresses or null if no valid source found
         */
        private static String[] getAddressOfWriter(Object writer) {
            while (true) {
                if (writer instanceof CommandExpiryWriter) {
                    writer = ((CommandExpiryWriter) writer).getDelegate();
                } else if (writer instanceof CommandListenerWriter) {
                    writer = ((CommandListenerWriter) writer).getDelegate();
                } else if (writer instanceof DefaultEndpoint) {
                    return getAddressOfEndpoint(writer);
                } else if (clusterDistributionType != null && clusterDistributionType.isInstance(writer)) {
                    return getAddressOfCluster(writer);
                } else if (masterReplicaType != null && masterReplicaType.isInstance(writer)) {
                    return getAddressOfMasterReplica(writer);
                } else {
                    return null;
                }
            }
        }

        /**
         * Extracts address from a DefaultEndpoint connection.
         *
         * @param target the endpoint object
         * @return address string or null
         */
        private static String[] getAddressOfEndpoint(Object target) {
            Channel ch = (Channel) channel.get(target);
            Attribute<String> attr = ch.attr(REDIS_URI);
            if (attr != null) {
                return getAddressOfUri(RedisURI.create(attr.get()));
            } else {
                SocketAddress address = ch.remoteAddress();
                if (address instanceof InetSocketAddress) {
                    InetSocketAddress isa = (InetSocketAddress) address;
                    return new String[]{Address.parse(isa.getHostString(), isa.getPort()).toString()};
                }
            }
            return null;
        }

        /**
         * Extracts addresses from a cluster distribution object.
         *
         * @param target the cluster distribution object
         * @return array of addresses or null
         */
        @SuppressWarnings("unchecked")
        private static String[] getAddressOfCluster(Object target) {
            Object provider = clusterConnectionProvider.get(target);
            RedisClusterClient client = null;
            if (clusterPubSubConnectionProviderType != null && clusterPubSubConnectionProviderType.isInstance(provider)) {
                client = (RedisClusterClient) clusterPubSubClient.get(provider);
            } else if (pooledClusterConnectionProvider != null && pooledClusterConnectionProvider.isInstance(provider)) {
                client = (RedisClusterClient) pooledClusterClient.get(provider);
            }
            if (client == null) {
                return null;
            }
            Iterable<RedisURI> uris = (Iterable<RedisURI>) initialUris.get(client);
            return toList(uris, uri -> Address.parse(uri.getHost(), uri.getPort()).toString()).toArray(new String[0]);
        }

        /**
         * Extracts address from a master-replica connection.
         *
         * @param target the master-replica connection object
         * @return address string or null
         */
        private static String[] getAddressOfMasterReplica(Object target) {
            Object provider = masterReplicaConnectionProvider.get(target);
            RedisURI uri = (RedisURI) initialRedisUri.get(provider);
            return getAddressOfUri(uri);
        }

        /**
         * Extracts Redis server addresses from the given RedisURI.
         *
         * @param uri RedisURI containing connection details
         * @return Array of server addresses (either direct or Sentinel nodes)
         */
        private static String[] getAddressOfUri(Object uri) {
            RedisURI redisURI = (RedisURI) uri;
            List<RedisURI> sentinels = redisURI.getSentinels();
            if (sentinels == null || sentinels.isEmpty()) {
                return new String[]{Address.parse(redisURI.getHost(), redisURI.getPort()).getAddress()};
            }
            String[] address = new String[sentinels.size()];
            int i = 0;
            for (RedisURI sentinel : sentinels) {
                address[i++] = Address.parse(sentinel.getHost(), sentinel.getPort()).getAddress();
            }
            return address;
        }

    }

}
