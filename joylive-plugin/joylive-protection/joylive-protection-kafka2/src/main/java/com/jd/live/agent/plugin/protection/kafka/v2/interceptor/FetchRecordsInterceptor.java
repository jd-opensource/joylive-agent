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
package com.jd.live.agent.plugin.protection.kafka.v2.interceptor;

import com.jd.live.agent.bootstrap.bytekit.context.ExecutableContext;
import com.jd.live.agent.bootstrap.bytekit.context.MethodContext;
import com.jd.live.agent.bootstrap.util.type.UnsafeFieldAccessor;
import com.jd.live.agent.governance.interceptor.AbstractMessageInterceptor;
import com.jd.live.agent.governance.invoke.InvocationContext;
import com.jd.live.agent.governance.invoke.auth.Permission;
import org.apache.kafka.clients.MetadataCache;
import org.apache.kafka.clients.consumer.internals.ConsumerMetadata;
import org.apache.kafka.clients.consumer.internals.Fetcher;
import org.apache.kafka.common.Node;
import org.apache.kafka.common.TopicPartition;

import java.util.Collections;
import java.util.Map;

import static com.jd.live.agent.bootstrap.util.type.UnsafeFieldAccessorFactory.getAccessor;
import static com.jd.live.agent.core.util.CollectionUtils.toList;

public class FetchRecordsInterceptor extends AbstractMessageInterceptor {

    public FetchRecordsInterceptor(InvocationContext context) {
        super(context);
    }

    @Override
    public void onEnter(ExecutableContext ctx) {
        TopicPartition partition = Accessors.getPartition(ctx.getArgument(0));
        if (partition != null) {
            ConsumerMetadata metadata = Accessors.getMetadata(ctx.getTarget());
            MetadataCache cache = Accessors.getCache(metadata);
            Map<Integer, Node> nodes = Accessors.getNodes(cache);
            String[] address = nodes == null ? null : toList(nodes.values(), this::toAddress).toArray(new String[0]);
            Permission permission = isConsumeReady(partition.topic(), null, address);
            if (!permission.isSuccess()) {
                ((MethodContext) ctx).skipWithResult(Collections.emptyList());
            }
        }
    }

    /**
     * Formats a node's address as "host:port" or "[IPv6]:port".
     *
     * @param node the cluster node containing host and port information
     * @return formatted address string with proper IPv6 bracketing
     * @throws NullPointerException if node or node.host() is null
     */
    protected String toAddress(Node node) {
        String hostName = node.host();
        if (hostName.contains(":")) {
            // ipv6
            return "[" + hostName + "]:" + node.port();
        }
        return hostName + ":" + node.port();
    }

    private static class Accessors {

        private static final UnsafeFieldAccessor partition;
        private static final UnsafeFieldAccessor metadata = getAccessor(Fetcher.class, "metadata");
        private static final UnsafeFieldAccessor cache = getAccessor(ConsumerMetadata.class, "cache");
        private static final UnsafeFieldAccessor nodes = getAccessor(MetadataCache.class, "nodes");

        static {
            UnsafeFieldAccessor accessor = null;
            try {
                Class<?> type = Class.forName("org.apache.kafka.clients.consumer.internals.Fetcher$CompletedFetch");
                accessor = getAccessor(type, "partition");
            } catch (ClassNotFoundException ignored) {
            }
            partition = accessor;
        }

        public static TopicPartition getPartition(Object target) {
            return partition == null || target == null ? null : (TopicPartition) partition.get(target);
        }

        public static ConsumerMetadata getMetadata(Object target) {
            return metadata == null || target == null ? null : (ConsumerMetadata) metadata.get(target);
        }

        public static MetadataCache getCache(Object target) {
            return cache == null || target == null ? null : (MetadataCache) cache.get(target);
        }

        @SuppressWarnings("unchecked")
        public static Map<Integer, Node> getNodes(Object target) {
            return nodes == null || target == null ? null : (Map<Integer, Node>) nodes.get(target);
        }

    }
}
