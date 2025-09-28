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
package com.jd.live.agent.plugin.failover.lettuce.v6.util;

import com.jd.live.agent.core.util.network.Address;
import io.lettuce.core.RedisURI;

import java.util.List;

import static com.jd.live.agent.core.util.CollectionUtils.toList;

public class UriUtils {

    /**
     * Builds a RedisURI by combining base configuration with target node address.
     * Uses default Redis port (6379) if not specified.
     *
     * @param uri     Base configuration (timeout, credentials, etc)
     * @param address Target node address (host:port format)
     * @return Configured RedisURI for direct node connection
     */
    public static RedisURI getUri(RedisURI uri, String address) {
        return builder(uri, Address.parse(address, true, RedisURI.DEFAULT_REDIS_PORT)).build();
    }

    /**
     * Formats the Redis server address as "host:port" string.
     * Handles both direct connections and Sentinel configurations.
     *
     * @param uri Connection details containing host/port
     * @return Formatted address string (e.g., "redis.example.com:6379")
     */
    public static String getAddress(RedisURI uri) {
        return Address.parse(uri.getHost(), uri.getPort()).getAddress();
    }

    /**
     * Creates a RedisURI configured with Sentinel nodes.
     *
     * @param uri   Base RedisURI containing connection parameters (timeout, credentials, etc)
     * @param nodes Array of Sentinel node addresses (e.g., ["sentinel1:26379", "sentinel2"])
     * @return New RedisURI configured with Sentinel topology
     */
    public static RedisURI getSentinelUri(RedisURI uri, String[] nodes) {
        RedisURI.Builder builder = builder(uri).withSentinelMasterId(uri.getSentinelMasterId());
        for (String node : nodes) {
            Address addr = Address.parse(node, true, RedisURI.DEFAULT_SENTINEL_PORT);
            builder = builder.withSentinel(addr.getHost(), addr.getPort());
        }
        return builder.build();
    }

    /**
     * Extracts Redis server addresses from the given RedisURI.
     *
     * @param uri RedisURI containing connection details
     * @return Array of server addresses (either direct or Sentinel nodes)
     */
    public static String[] getSentinelAddress(RedisURI uri) {
        List<RedisURI> sentinels = uri.getSentinels();
        if (sentinels == null || sentinels.isEmpty()) {
            return new String[]{Address.parse(uri.getHost(), uri.getPort()).getAddress()};
        }
        String[] address = new String[sentinels.size()];
        int i = 0;
        for (RedisURI sentinel : sentinels) {
            address[i++] = Address.parse(sentinel.getHost(), sentinel.getPort()).getAddress();
        }
        return address;
    }

    /**
     * Creates cluster RedisURIs by combining base config with individual node addresses.
     *
     * @param uri   Base configuration (common settings)
     * @param nodes Node addresses as "host[:port]" strings
     * @return Configured RedisURI instances for each node
     */
    public static List<RedisURI> getClusterUris(RedisURI uri, String[] nodes) {
        return toList(nodes, node -> builder(uri, Address.parse(node, true, RedisURI.DEFAULT_REDIS_PORT)).build());
    }

    /**
     * Extracts "host:port" addresses from RedisURI instances.
     *
     * @param uris Redis cluster node configurations
     * @return Array of node address strings
     */
    public static String[] getClusterAddress(Iterable<RedisURI> uris) {
        return toList(uris, u -> Address.parse(u.getHost(), u.getPort()).getAddress()).toArray(new String[0]);
    }

    /**
     * Creates a new {@link RedisURI.Builder} pre-configured from an existing {@link RedisURI}.
     * Copies authentication, SSL settings, database, timeout, and peer verification settings.
     *
     * @param uri the source RedisURI to copy configuration from
     * @return a new builder initialized with the source URI's settings
     */
    public static RedisURI.Builder builder(RedisURI uri) {
        return RedisURI.builder(uri)
                .withAuthentication(uri)
                .withSsl(uri)
                .withDatabase(uri.getDatabase())
                .withTimeout(uri.getTimeout())
                .withVerifyPeer(uri.isVerifyPeer())
                .withVerifyPeer(uri.getVerifyMode());
    }

    /**
     * Creates a new {@link RedisURI.Builder} from an existing {@link RedisURI} with updated address.
     *
     * @param uri     the source RedisURI to copy configuration from
     * @param address the new address to use
     * @return a new builder with the source URI's settings and updated address
     */
    public static RedisURI.Builder builder(RedisURI uri, Address address) {
        return builder(uri).withHost(address.getHost()).withPort(address.getPort());
    }
}
