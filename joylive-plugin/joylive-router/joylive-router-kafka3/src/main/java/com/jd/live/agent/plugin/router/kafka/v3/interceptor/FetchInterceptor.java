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
package com.jd.live.agent.plugin.router.kafka.v3.interceptor;

import com.jd.live.agent.bootstrap.bytekit.context.ExecutableContext;
import com.jd.live.agent.governance.interceptor.AbstractMessageInterceptor;
import com.jd.live.agent.governance.invoke.InvocationContext;
import com.jd.live.agent.plugin.router.kafka.v3.message.KafkaConsumerMessage;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.TopicPartition;

import java.util.List;

import static com.jd.live.agent.core.util.CollectionUtils.filter;

public class FetchInterceptor extends AbstractMessageInterceptor {

    public FetchInterceptor(InvocationContext context) {
        super(context);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onEnter(ExecutableContext ctx) {
        Object[] arguments = ctx.getArguments();
        TopicPartition topicPartition = (TopicPartition) arguments[0];
        String topic = getSource(topicPartition.topic());
        if (isEnabled(topic)) {
            List<ConsumerRecord<?, ?>> records = (List<ConsumerRecord<?, ?>>) arguments[1];
            filter(records, message -> allow(new KafkaConsumerMessage(message)) == MessageAction.CONSUME);
        }
    }
}
