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
import com.jd.live.agent.core.event.Publisher;
import com.jd.live.agent.core.extension.annotation.ConditionalOnClass;
import com.jd.live.agent.core.extension.annotation.Extension;
import com.jd.live.agent.core.inject.annotation.Inject;
import com.jd.live.agent.core.inject.annotation.Injectable;
import com.jd.live.agent.core.plugin.definition.InterceptorDefinition;
import com.jd.live.agent.core.plugin.definition.InterceptorDefinitionAdapter;
import com.jd.live.agent.core.plugin.definition.PluginDefinition;
import com.jd.live.agent.core.plugin.definition.PluginDefinitionAdapter;
import com.jd.live.agent.core.util.time.Timer;
import com.jd.live.agent.governance.event.DatabaseEvent;
import com.jd.live.agent.governance.policy.PolicySupplier;
import com.jd.live.agent.plugin.protection.rocketmq.v4.condition.ConditionalOnRocketmq4ProtectEnabled;
import com.jd.live.agent.plugin.protection.rocketmq.v4.interceptor.DefaultLitePullConsumerInterceptor;

@Injectable
@Extension(value = "DefaultLitePullConsumerDefinition_v4", order = PluginDefinition.ORDER_PROTECT)
@ConditionalOnRocketmq4ProtectEnabled
@ConditionalOnClass(DefaultLitePullConsumerDefinition.TYPE)
public class DefaultLitePullConsumerDefinition extends PluginDefinitionAdapter {

    protected static final String TYPE = "org.apache.rocketmq.client.consumer.DefaultLitePullConsumer";

    private static final String METHOD = "start";

    @Inject(PolicySupplier.COMPONENT_POLICY_SUPPLIER)
    private PolicySupplier policySupplier;

    @Inject(Publisher.DATABASE)
    private Publisher<DatabaseEvent> publisher;

    @Inject(Timer.COMPONENT_TIMER)
    private Timer timer;

    public DefaultLitePullConsumerDefinition() {
        this.matcher = () -> MatcherBuilder.named(TYPE);
        this.interceptors = new InterceptorDefinition[]{
                new InterceptorDefinitionAdapter(
                        MatcherBuilder.named(METHOD),
                        () -> new DefaultLitePullConsumerInterceptor(policySupplier, publisher, timer)
                )
        };
    }
}
