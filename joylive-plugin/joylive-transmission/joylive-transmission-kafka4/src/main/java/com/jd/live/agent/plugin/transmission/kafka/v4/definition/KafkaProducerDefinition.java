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
package com.jd.live.agent.plugin.transmission.kafka.v4.definition;

import com.jd.live.agent.core.bytekit.matcher.MatcherBuilder;
import com.jd.live.agent.core.extension.annotation.ConditionalOnClass;
import com.jd.live.agent.core.extension.annotation.Extension;
import com.jd.live.agent.core.inject.annotation.Inject;
import com.jd.live.agent.core.inject.annotation.Injectable;
import com.jd.live.agent.core.plugin.definition.InterceptorDefinition;
import com.jd.live.agent.core.plugin.definition.InterceptorDefinitionAdapter;
import com.jd.live.agent.core.plugin.definition.PluginDefinition;
import com.jd.live.agent.core.plugin.definition.PluginDefinitionAdapter;
import com.jd.live.agent.governance.invoke.InvocationContext;
import com.jd.live.agent.plugin.transmission.kafka.v4.condition.ConditionalOnKafka4TransmissionEnabled;
import com.jd.live.agent.plugin.transmission.kafka.v4.interceptor.KafkaProducerInterceptor;

@Injectable
@Extension(value = "KafkaProducerDefinition_v3", order = PluginDefinition.ORDER_TRANSMISSION)
@ConditionalOnKafka4TransmissionEnabled
@ConditionalOnClass(KafkaProducerDefinition.TYPE_KAFKA_PRODUCER)
public class KafkaProducerDefinition extends PluginDefinitionAdapter {
    public static final String TYPE_KAFKA_PRODUCER = "org.apache.kafka.clients.producer.KafkaProducer";

    private static final String METHOD_DO_SEND = "doSend";

    private static final String[] ARGUMENT_DO_SEND = new String[]{
            "org.apache.kafka.clients.producer.ProducerRecord",
            "org.apache.kafka.clients.producer.Callback"
    };

    @Inject(InvocationContext.COMPONENT_INVOCATION_CONTEXT)
    private InvocationContext context;

    public KafkaProducerDefinition() {
        this.matcher = () -> MatcherBuilder.named(TYPE_KAFKA_PRODUCER);
        this.interceptors = new InterceptorDefinition[]{
                new InterceptorDefinitionAdapter(
                        MatcherBuilder.named(METHOD_DO_SEND).
                                and(MatcherBuilder.arguments(ARGUMENT_DO_SEND)),
                        () -> new KafkaProducerInterceptor(context))};
    }
}
