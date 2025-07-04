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
package com.jd.live.agent.plugin.protection.kafka.v4.interceptor;

import com.jd.live.agent.bootstrap.bytekit.context.ExecutableContext;
import com.jd.live.agent.bootstrap.bytekit.context.MethodContext;
import com.jd.live.agent.bootstrap.util.type.UnsafeFieldAccessor;
import com.jd.live.agent.core.util.network.Ipv4;
import com.jd.live.agent.governance.interceptor.AbstractMessageInterceptor;
import com.jd.live.agent.governance.invoke.InvocationContext;
import com.jd.live.agent.governance.invoke.auth.Permission;
import org.apache.kafka.clients.consumer.internals.CompletedFetch;
import org.apache.kafka.clients.consumer.internals.ConsumerMetadata;
import org.apache.kafka.clients.consumer.internals.Fetch;
import org.apache.kafka.clients.consumer.internals.FetchCollector;
import org.apache.kafka.common.TopicPartition;

import java.net.InetSocketAddress;
import java.util.List;

import static com.jd.live.agent.bootstrap.util.type.UnsafeFieldAccessorFactory.getAccessor;
import static com.jd.live.agent.core.util.CollectionUtils.toList;

public class FetchRecordsInterceptor extends AbstractMessageInterceptor {

    public FetchRecordsInterceptor(InvocationContext context) {
        super(context);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onEnter(ExecutableContext ctx) {
        TopicPartition partition = Accessors.partition.get(ctx.getArgument(0), TopicPartition.class);
        if (partition != null) {
            ConsumerMetadata metadata = Accessors.metadata.get(ctx.getTarget(), ConsumerMetadata.class);
            // TODO addresses
            List<InetSocketAddress> bootstrapAddresses = (List<InetSocketAddress>) Accessors.bootstrapAddresses.get(metadata);
            String[] address = bootstrapAddresses == null ? null : toList(bootstrapAddresses, Ipv4::toString).toArray(new String[0]);
            Permission permission = isConsumeReady(partition.topic(), null, address);
            if (!permission.isSuccess()) {
                ((MethodContext) ctx).skipWithResult(Fetch.empty());
            }
        }
    }

    private static class Accessors {
        private static final UnsafeFieldAccessor partition = getAccessor(CompletedFetch.class, "partition");
        private static final UnsafeFieldAccessor metadata = getAccessor(FetchCollector.class, "metadata");
        private static final UnsafeFieldAccessor bootstrapAddresses = getAccessor(ConsumerMetadata.class, "bootstrapAddresses");
    }
}
