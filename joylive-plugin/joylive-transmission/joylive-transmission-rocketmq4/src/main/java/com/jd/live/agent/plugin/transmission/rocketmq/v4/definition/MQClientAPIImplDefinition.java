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
package com.jd.live.agent.plugin.transmission.rocketmq.v4.definition;

import com.jd.live.agent.core.bytekit.matcher.MatcherBuilder;
import com.jd.live.agent.core.extension.annotation.*;
import com.jd.live.agent.core.plugin.definition.InterceptorDefinitionAdapter;
import com.jd.live.agent.core.plugin.definition.PluginDefinition;
import com.jd.live.agent.core.plugin.definition.PluginDefinitionAdapter;
import com.jd.live.agent.governance.config.GovernanceConfig;
import com.jd.live.agent.plugin.transmission.rocketmq.v4.interceptor.MQClientAPIImplInterceptor;

@Extension(value = "MQClientAPIImplDefinition_v4", order = PluginDefinition.ORDER_TRANSMISSION)
@ConditionalOnProperties(value = {
        @ConditionalOnProperty(value = GovernanceConfig.CONFIG_LIVE_ENABLED, matchIfMissing = true),
        @ConditionalOnProperty(value = GovernanceConfig.CONFIG_LANE_ENABLED, matchIfMissing = true)
}, relation = ConditionalRelation.OR)
@ConditionalOnClass(MQClientAPIImplDefinition.TYPE_MQ_CLIENT_API_IMPL)
@ConditionalOnClass(MessageDefinition.TYPE_CLIENT_LOGGER)
public class MQClientAPIImplDefinition extends PluginDefinitionAdapter {

    protected static final String TYPE_MQ_CLIENT_API_IMPL = "org.apache.rocketmq.client.impl.MQClientAPIImpl";

    private static final String METHOD_SEND_MESSAGE = "sendMessage";

    private static final String[] ARGUMENT_SEND_MESSAGE = {
            "java.lang.String",
            "java.lang.String",
            "org.apache.rocketmq.common.message.Message",
            "org.apache.rocketmq.common.protocol.header.SendMessageRequestHeader",
            "long",
            "org.apache.rocketmq.client.impl.CommunicationMode",
            "org.apache.rocketmq.client.producer.SendCallback",
            "org.apache.rocketmq.client.impl.producer.TopicPublishInfo",
            "org.apache.rocketmq.client.impl.factory.MQClientInstance",
            "int",
            "org.apache.rocketmq.client.hook.SendMessageContext",
            "org.apache.rocketmq.client.impl.producer.DefaultMQProducerImpl"
    };

    public MQClientAPIImplDefinition() {
        super(TYPE_MQ_CLIENT_API_IMPL,
                new InterceptorDefinitionAdapter(
                        MatcherBuilder.named(METHOD_SEND_MESSAGE).
                                and(MatcherBuilder.arguments(ARGUMENT_SEND_MESSAGE)),
                        new MQClientAPIImplInterceptor()));
    }

}
