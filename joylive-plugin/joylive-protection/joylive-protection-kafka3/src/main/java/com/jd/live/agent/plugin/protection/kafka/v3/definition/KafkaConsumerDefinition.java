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
package com.jd.live.agent.plugin.protection.kafka.v3.definition;

import com.jd.live.agent.core.bytekit.matcher.MatcherBuilder;
import com.jd.live.agent.core.extension.annotation.ConditionalOnClass;
import com.jd.live.agent.core.extension.annotation.Extension;
import com.jd.live.agent.core.inject.annotation.Injectable;
import com.jd.live.agent.core.plugin.definition.InterceptorDefinition;
import com.jd.live.agent.core.plugin.definition.InterceptorDefinitionAdapter;
import com.jd.live.agent.core.plugin.definition.PluginDefinitionAdapter;
import com.jd.live.agent.plugin.protection.kafka.v3.condition.ConditionalOnProtectKafka3Enabled;
import com.jd.live.agent.plugin.protection.kafka.v3.interceptor.KafkaConsumerInterceptor;

/**
 * KafkaConsumerDefinition
 *
 * @since 1.8.0
 */
@Injectable
@Extension(value = "KafkaConsumerDefinition_v3")
@ConditionalOnProtectKafka3Enabled
@ConditionalOnClass(KafkaConsumerDefinition.TYPE)
public class KafkaConsumerDefinition extends PluginDefinitionAdapter {

    protected static final String TYPE = "org.apache.kafka.clients.consumer.KafkaConsumer";

    private static final String[] ARGUMENTS = new String[]{
            "org.apache.kafka.clients.consumer.ConsumerConfig",
            "org.apache.kafka.common.serialization.Deserializer",
            "org.apache.kafka.common.serialization.Deserializer",
    };

    public KafkaConsumerDefinition() {
        this.matcher = () -> MatcherBuilder.named(TYPE);
        this.interceptors = new InterceptorDefinition[]{
                new InterceptorDefinitionAdapter(
                        MatcherBuilder.isConstructor().and(MatcherBuilder.arguments(ARGUMENTS))
                        , () -> new KafkaConsumerInterceptor()
                )
        };
    }
}
