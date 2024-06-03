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
package com.jd.live.agent.plugin.transmission.rocketmq.v5.definition;

import com.jd.live.agent.core.bytekit.matcher.MatcherBuilder;
import com.jd.live.agent.core.extension.annotation.*;
import com.jd.live.agent.core.plugin.definition.InterceptorDefinitionAdapter;
import com.jd.live.agent.core.plugin.definition.PluginDefinition;
import com.jd.live.agent.core.plugin.definition.PluginDefinitionAdapter;
import com.jd.live.agent.governance.config.GovernanceConfig;
import com.jd.live.agent.plugin.transmission.rocketmq.v5.interceptor.MQProducerInterceptor;

@Extension(value = "MQProducerDefinition_v5", order = PluginDefinition.ORDER_TRANSMISSION)
@ConditionalOnProperties(value = {
        @ConditionalOnProperty(value = GovernanceConfig.CONFIG_LIVE_ENABLED, matchIfMissing = true),
        @ConditionalOnProperty(value = GovernanceConfig.CONFIG_LANE_ENABLED, matchIfMissing = true)
}, relation = ConditionalRelation.OR)
@ConditionalOnClass(MQProducerDefinition.TYPE_MQ_PRODUCER)
@ConditionalOnClass(MessageDefinition.TYPE_ACK_CALLBACK)
public class MQProducerDefinition extends PluginDefinitionAdapter {

    protected static final String TYPE_MQ_PRODUCER = "org.apache.rocketmq.client.producer.MQProducer";

    private static final String METHOD_SEND = "send";

    private static final String METHOD_REQUEST = "request";

    private static final String METHOD_SEND_MESSAGE_IN_TRANSACTION = "sendMessageInTransaction";

    private static final String METHOD_SEND_ONEWAY = "sendOneway";

    public MQProducerDefinition() {
        super(MatcherBuilder.isImplement(TYPE_MQ_PRODUCER),
                new InterceptorDefinitionAdapter(MatcherBuilder.named(METHOD_SEND), new MQProducerInterceptor()),
                new InterceptorDefinitionAdapter(MatcherBuilder.named(METHOD_REQUEST), new MQProducerInterceptor()),
                new InterceptorDefinitionAdapter(MatcherBuilder.named(METHOD_SEND_ONEWAY), new MQProducerInterceptor()),
                new InterceptorDefinitionAdapter(MatcherBuilder.named(METHOD_SEND_MESSAGE_IN_TRANSACTION), new MQProducerInterceptor()));
    }
}
