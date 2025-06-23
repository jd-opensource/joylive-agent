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
package com.jd.live.agent.plugin.protection.rocketmq.v5.definition;

import com.jd.live.agent.core.bytekit.matcher.MatcherBuilder;
import com.jd.live.agent.core.extension.annotation.ConditionalOnClass;
import com.jd.live.agent.core.extension.annotation.Extension;
import com.jd.live.agent.core.inject.annotation.Inject;
import com.jd.live.agent.core.inject.annotation.Injectable;
import com.jd.live.agent.core.plugin.definition.InterceptorDefinition;
import com.jd.live.agent.core.plugin.definition.InterceptorDefinitionAdapter;
import com.jd.live.agent.core.plugin.definition.PluginDefinitionAdapter;
import com.jd.live.agent.governance.invoke.InvocationContext;
import com.jd.live.agent.plugin.protection.rocketmq.v5.condition.ConditionalOnRocketmq5AnyRouteEnabled;
import com.jd.live.agent.plugin.protection.rocketmq.v5.interceptor.SendInterceptor;

/**
 * DefaultMQProducerImplDefinition
 *
 * @since 1.8.0
 */
@Injectable
@Extension(value = "DefaultMQProducerImplDefinition_v5")
@ConditionalOnRocketmq5AnyRouteEnabled
@ConditionalOnClass(DefaultMQProducerImplDefinition.TYPE)
public class DefaultMQProducerImplDefinition extends PluginDefinitionAdapter {

    protected static final String TYPE = "org.apache.rocketmq.client.impl.producer.DefaultMQProducerImpl";

    private static final String METHOD = "sendDefaultImpl";

    private static final String[] ARGUMENTS = new String[]{
            "org.apache.rocketmq.common.message.Message",
            "org.apache.rocketmq.client.impl.CommunicationMode",
            "org.apache.rocketmq.client.producer.SendCallback",
            "long"
    };

    @Inject(InvocationContext.COMPONENT_INVOCATION_CONTEXT)
    private InvocationContext context;

    public DefaultMQProducerImplDefinition() {
        this.matcher = () -> MatcherBuilder.named(TYPE);
        this.interceptors = new InterceptorDefinition[]{
                new InterceptorDefinitionAdapter(
                        MatcherBuilder.named(METHOD).and(MatcherBuilder.arguments(ARGUMENTS)),
                        () -> new SendInterceptor(context)
                )
        };
    }
}
