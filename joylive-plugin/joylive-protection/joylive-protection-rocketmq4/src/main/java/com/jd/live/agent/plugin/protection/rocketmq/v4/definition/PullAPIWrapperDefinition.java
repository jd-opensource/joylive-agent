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
package com.jd.live.agent.plugin.protection.rocketmq.v4.definition;

import com.jd.live.agent.core.bytekit.matcher.MatcherBuilder;
import com.jd.live.agent.core.extension.annotation.ConditionalOnClass;
import com.jd.live.agent.core.extension.annotation.Extension;
import com.jd.live.agent.core.inject.annotation.Inject;
import com.jd.live.agent.core.inject.annotation.Injectable;
import com.jd.live.agent.core.plugin.definition.InterceptorDefinition;
import com.jd.live.agent.core.plugin.definition.InterceptorDefinitionAdapter;
import com.jd.live.agent.core.plugin.definition.PluginDefinitionAdapter;
import com.jd.live.agent.governance.invoke.InvocationContext;
import com.jd.live.agent.plugin.protection.rocketmq.v4.condition.ConditionalOnProtectRocketmq4Enabled;
import com.jd.live.agent.plugin.protection.rocketmq.v4.interceptor.PullInterceptor;

/**
 * PullAPIWrapperDefinition
 *
 * @since 1.8.0
 */
@Injectable
@Extension(value = "PullAPIWrapperDefinition_v4")
@ConditionalOnProtectRocketmq4Enabled
@ConditionalOnClass(PullAPIWrapperDefinition.TYPE)
public class PullAPIWrapperDefinition extends PluginDefinitionAdapter {

    protected static final String TYPE = "org.apache.rocketmq.client.impl.consumer.PullAPIWrapper";

    private static final String METHOD = "pullKernelImpl";

    private static final String[] ARGUMENTS = new String[]{
            "org.apache.rocketmq.common.message.MessageQueue",
            "java.lang.String",
            "java.lang.String",
            "long",
            "long",
            "int",
            "int",
            "long",
            "long",
            "long",
            "org.apache.rocketmq.client.impl.CommunicationMode",
            "org.apache.rocketmq.client.consumer.PullCallback"
    };

    @Inject(InvocationContext.COMPONENT_INVOCATION_CONTEXT)
    private InvocationContext context;

    public PullAPIWrapperDefinition() {
        this.matcher = () -> MatcherBuilder.named(TYPE);
        this.interceptors = new InterceptorDefinition[]{
                new InterceptorDefinitionAdapter(
                        MatcherBuilder.named(METHOD).and(MatcherBuilder.arguments(ARGUMENTS)),
                        () -> new PullInterceptor(context)
                )
        };
    }
}
