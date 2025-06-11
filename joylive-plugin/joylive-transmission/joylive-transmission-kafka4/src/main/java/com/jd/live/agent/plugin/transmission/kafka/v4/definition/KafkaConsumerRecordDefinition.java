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
import com.jd.live.agent.governance.context.bag.Propagation;
import com.jd.live.agent.plugin.transmission.kafka.v4.condition.ConditionalOnKafka4TransmissionEnabled;
import com.jd.live.agent.plugin.transmission.kafka.v4.interceptor.KafkaConsumerRecordInterceptor;

@Injectable
@Extension(value = "KafkaConsumerRecordDefinition_v3", order = PluginDefinition.ORDER_TRANSMISSION)
@ConditionalOnKafka4TransmissionEnabled
@ConditionalOnClass(KafkaConsumerRecordDefinition.TYPE_CONSUMER_RECORD)
public class KafkaConsumerRecordDefinition extends PluginDefinitionAdapter {
    public static final String TYPE_CONSUMER_RECORD = "org.apache.kafka.clients.consumer.ConsumerRecord";

    private static final String METHOD_VALUE = "value";

    @Inject(value = Propagation.COMPONENT_PROPAGATION, component = true)
    private Propagation propagation;

    public KafkaConsumerRecordDefinition() {
        this.matcher = () -> MatcherBuilder.named(TYPE_CONSUMER_RECORD);
        this.interceptors = new InterceptorDefinition[]{
                new InterceptorDefinitionAdapter(
                        MatcherBuilder.named(METHOD_VALUE).
                                and(MatcherBuilder.arguments(0)),
                        () -> new KafkaConsumerRecordInterceptor(propagation))};
    }

}
