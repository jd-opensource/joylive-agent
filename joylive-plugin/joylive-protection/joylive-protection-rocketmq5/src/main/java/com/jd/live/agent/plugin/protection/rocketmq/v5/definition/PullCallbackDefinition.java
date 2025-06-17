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
import com.jd.live.agent.core.plugin.definition.InterceptorDefinition;
import com.jd.live.agent.core.plugin.definition.InterceptorDefinitionAdapter;
import com.jd.live.agent.core.plugin.definition.PluginDefinition;
import com.jd.live.agent.core.plugin.definition.PluginDefinitionAdapter;
import com.jd.live.agent.plugin.protection.rocketmq.v5.condition.ConditionalOnFailoverRocketmq5Enabled;
import com.jd.live.agent.plugin.protection.rocketmq.v5.interceptor.PullCallbackInterceptor;

@Extension(value = "PullCallbackDefinition_v5", order = PluginDefinition.ORDER_PROTECT)
@ConditionalOnFailoverRocketmq5Enabled
@ConditionalOnClass(PullCallbackDefinition.TYPE)
public class PullCallbackDefinition extends PluginDefinitionAdapter {

    protected static final String TYPE = "org.apache.rocketmq.client.impl.consumer.DefaultMQPushConsumerImpl$1";

    private static final String METHOD = "onException";

    public PullCallbackDefinition() {
        this.matcher = () -> MatcherBuilder.named(TYPE);
        this.interceptors = new InterceptorDefinition[]{
                new InterceptorDefinitionAdapter(
                        MatcherBuilder.named(METHOD),
                        () -> new PullCallbackInterceptor()
                )
        };
    }
}
