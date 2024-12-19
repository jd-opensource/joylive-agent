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
package com.jd.live.agent.plugin.router.rocketmq.v5.definition;

import com.jd.live.agent.core.bytekit.matcher.MatcherBuilder;
import com.jd.live.agent.core.extension.annotation.ConditionalOnClass;
import com.jd.live.agent.core.extension.annotation.Extension;
import com.jd.live.agent.core.inject.annotation.Inject;
import com.jd.live.agent.core.inject.annotation.Injectable;
import com.jd.live.agent.core.plugin.definition.InterceptorDefinition;
import com.jd.live.agent.core.plugin.definition.InterceptorDefinitionAdapter;
import com.jd.live.agent.core.plugin.definition.PluginDefinitionAdapter;
import com.jd.live.agent.governance.invoke.InvocationContext;
import com.jd.live.agent.plugin.router.rocketmq.v5.condition.ConditionalOnRocketmq5AnyRouteEnabled;
import com.jd.live.agent.plugin.router.rocketmq.v5.interceptor.GroupInterceptor;

/**
 * MQPushConsumerDefinition
 *
 * @since 1.0.0
 */
@Injectable
@Extension(value = "MQPushConsumerDefinition_v5")
@ConditionalOnRocketmq5AnyRouteEnabled
@ConditionalOnClass(MQPushConsumerDefinition.TYPE_MQ_PUSH_CONSUMER)
public class MQPushConsumerDefinition extends PluginDefinitionAdapter {

    protected static final String TYPE_MQ_PUSH_CONSUMER = "org.apache.rocketmq.client.consumer.MQPushConsumer";

    private static final String METHOD_SET_CONSUMER_GROUP = "setConsumerGroup";

    @Inject(InvocationContext.COMPONENT_INVOCATION_CONTEXT)
    private InvocationContext context;

    public MQPushConsumerDefinition() {
        this.matcher = () -> MatcherBuilder.isImplement(TYPE_MQ_PUSH_CONSUMER);
        this.interceptors = new InterceptorDefinition[]{
                new InterceptorDefinitionAdapter(
                        MatcherBuilder.named(METHOD_SET_CONSUMER_GROUP),
                        () -> new GroupInterceptor(context)
                )
        };
    }
}
