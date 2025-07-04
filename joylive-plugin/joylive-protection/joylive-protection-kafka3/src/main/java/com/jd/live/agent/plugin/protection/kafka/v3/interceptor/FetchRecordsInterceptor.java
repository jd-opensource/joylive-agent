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
package com.jd.live.agent.plugin.protection.kafka.v3.interceptor;

import com.jd.live.agent.bootstrap.bytekit.context.ExecutableContext;
import com.jd.live.agent.bootstrap.bytekit.context.MethodContext;
import com.jd.live.agent.bootstrap.util.type.UnsafeFieldAccessor;
import com.jd.live.agent.core.util.network.Address;
import com.jd.live.agent.governance.interceptor.AbstractMessageInterceptor;
import com.jd.live.agent.governance.invoke.InvocationContext;
import com.jd.live.agent.governance.invoke.auth.Permission;
import org.apache.kafka.clients.MetadataCache;
import org.apache.kafka.clients.consumer.internals.ConsumerMetadata;
import org.apache.kafka.clients.consumer.internals.Fetch;
import org.apache.kafka.clients.consumer.internals.Fetcher;
import org.apache.kafka.common.Node;
import org.apache.kafka.common.TopicPartition;

import java.util.Map;

import static com.jd.live.agent.bootstrap.util.type.UnsafeFieldAccessorFactory.getAccessor;
import static com.jd.live.agent.core.util.CollectionUtils.toList;
import static com.jd.live.agent.core.util.type.ClassUtils.loadClass;

public class FetchRecordsInterceptor extends AbstractMessageInterceptor {

    public FetchRecordsInterceptor(InvocationContext context) {
        super(context);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onEnter(ExecutableContext ctx) {
        // TODO address
        TopicPartition partition = Accessors.partition.get(ctx.getArgument(0), TopicPartition.class);
        if (partition != null) {
            ConsumerMetadata metadata = Accessors.metadata.get(ctx.getTarget(), ConsumerMetadata.class);
            MetadataCache cache = Accessors.cache.get(metadata, MetadataCache.class);
            Map<Integer, Node> nodes = (Map<Integer, Node>) Accessors.nodes.get(cache);
            String[] address = nodes == null ? null : toList(nodes.values(), this::toAddress).toArray(new String[0]);
            Permission permission = isConsumeReady(partition.topic(), null, address);
            if (!permission.isSuccess()) {
                ((MethodContext) ctx).skipWithResult(Fetch.empty());
            }
        }
    }

    /**
     * Formats a node's address as "host:port" or "[IPv6]:port".
     *
     * @param node the cluster node containing host and port information
     * @return formatted address string with proper IPv6 bracketing
     */
    protected String toAddress(Node node) {
        return Address.parse(node.host(), node.port()).getAddress();
    }

    private static class Accessors {
        private static final Class<?> completedFetchType = loadClass("org.apache.kafka.clients.consumer.internals.Fetcher$CompletedFetch", Fetcher.class.getClassLoader());
        private static final UnsafeFieldAccessor partition = getAccessor(completedFetchType, "partition");
        private static final UnsafeFieldAccessor metadata = getAccessor(Fetcher.class, "metadata");
        private static final UnsafeFieldAccessor cache = getAccessor(ConsumerMetadata.class, "cache");
        private static final UnsafeFieldAccessor nodes = getAccessor(MetadataCache.class, "nodes");
    }
}
