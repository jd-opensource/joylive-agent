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
import com.jd.live.agent.plugin.protection.kafka.v2.client.LiveKafkaClient;
import org.apache.kafka.clients.KafkaClient;
import org.apache.kafka.clients.consumer.internals.ConsumerNetworkClient;
import org.apache.kafka.clients.consumer.internals.Fetcher;
import org.apache.kafka.common.TopicPartition;

import java.util.Collections;

import static com.jd.live.agent.bootstrap.util.type.UnsafeFieldAccessorFactory.getAccessor;
import static com.jd.live.agent.core.util.type.ClassUtils.loadClass;

public class FetchRecordsInterceptor extends AbstractMessageInterceptor {

    public FetchRecordsInterceptor(InvocationContext context) {
        super(context);
    }

    @Override
    public void onEnter(ExecutableContext ctx) {
        TopicPartition partition = Accessors.partition.get(ctx.getArgument(0), TopicPartition.class);
        if (partition != null) {
            Fetcher<?, ?> fetcher = (Fetcher<?, ?>) ctx.getTarget();
            ConsumerNetworkClient networkClient = Accessors.networkClient.get(fetcher, ConsumerNetworkClient.class);
            KafkaClient kafkaClient = Accessors.kafkaClient.get(networkClient, KafkaClient.class);
            String[] addresses = kafkaClient instanceof LiveKafkaClient ? ((LiveKafkaClient) kafkaClient).getAddresses() : null;
            Permission permission = isConsumeReady(partition.topic(), null, addresses);
            if (!permission.isSuccess()) {
                ((MethodContext) ctx).skipWithResult(Collections.emptyList());
            }
        }
    }

    private static class Accessors {
        private static final Class<?> completedFetchType = loadClass("org.apache.kafka.clients.consumer.internals.Fetcher$CompletedFetch", Fetcher.class.getClassLoader());
        private static final UnsafeFieldAccessor partition = getAccessor(completedFetchType, "partition");
        private static final UnsafeFieldAccessor networkClient = getAccessor(Fetcher.class, "client");
        private static final UnsafeFieldAccessor kafkaClient = getAccessor(ConsumerNetworkClient.class, "client");
    }
}
