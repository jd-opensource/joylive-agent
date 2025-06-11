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
package com.jd.live.agent.plugin.router.kafka.v2.interceptor;

import com.jd.live.agent.bootstrap.bytekit.context.ExecutableContext;
import com.jd.live.agent.bootstrap.bytekit.context.MethodContext;
import com.jd.live.agent.governance.interceptor.AbstractMessageInterceptor;
import com.jd.live.agent.governance.invoke.InvocationContext;
import com.jd.live.agent.plugin.router.kafka.v2.message.KafkaMessage;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.TopicPartition;

import java.util.Collections;
import java.util.List;

import static com.jd.live.agent.bootstrap.util.type.UnsafeFieldAccessorFactory.getQuietly;
import static com.jd.live.agent.core.util.CollectionUtils.filter;

public class FetcherInterceptor extends AbstractMessageInterceptor {

    private static final String FIELD_PARTITION = "partition";

    public FetcherInterceptor(InvocationContext context) {
        super(context);
    }

    @Override
    public void onEnter(ExecutableContext ctx) {
        TopicPartition partition = getQuietly(ctx.getArgument(0), FIELD_PARTITION);
        if (partition != null) {
            ctx.setAttribute(FIELD_PARTITION, partition);
            if (!isConsumeReady(partition.topic())) {
                MethodContext mc = (MethodContext) ctx;
                mc.setResult(Collections.emptyList());
                mc.setSkip(true);
            }
        }
    }

    @Override
    public void onSuccess(ExecutableContext ctx) {
        MethodContext mc = (MethodContext) ctx;
        TopicPartition partition = ctx.getAttribute(FIELD_PARTITION);
        if (partition != null) {
            if (isEnabled(partition.topic())) {
                List<ConsumerRecord<?, ?>> records = mc.getResult();
                filter(records, message -> consume(new KafkaMessage(message)) == MessageAction.CONSUME);
            }
        }
    }
}
