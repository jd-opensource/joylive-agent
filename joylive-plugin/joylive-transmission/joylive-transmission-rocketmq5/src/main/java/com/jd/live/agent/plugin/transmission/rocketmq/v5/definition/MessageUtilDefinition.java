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

import com.jd.live.agent.bootstrap.classloader.ResourcerType;
import com.jd.live.agent.core.bytekit.matcher.MatcherBuilder;
import com.jd.live.agent.core.extension.annotation.*;
import com.jd.live.agent.core.inject.annotation.Inject;
import com.jd.live.agent.core.inject.annotation.InjectLoader;
import com.jd.live.agent.core.inject.annotation.Injectable;
import com.jd.live.agent.core.plugin.definition.InterceptorDefinition;
import com.jd.live.agent.core.plugin.definition.InterceptorDefinitionAdapter;
import com.jd.live.agent.core.plugin.definition.PluginDefinition;
import com.jd.live.agent.core.plugin.definition.PluginDefinitionAdapter;
import com.jd.live.agent.governance.config.GovernanceConfig;
import com.jd.live.agent.governance.context.bag.CargoRequire;
import com.jd.live.agent.plugin.transmission.rocketmq.v5.interceptor.MessageUtilInterceptor;

import java.util.List;

@Injectable
@Extension(value = "MessageUtilDefinition_v5", order = PluginDefinition.ORDER_TRANSMISSION)
@ConditionalOnProperties(value = {
        @ConditionalOnProperty(value = GovernanceConfig.CONFIG_LIVE_ENABLED, matchIfMissing = true),
        @ConditionalOnProperty(value = GovernanceConfig.CONFIG_LANE_ENABLED, matchIfMissing = true)
}, relation = ConditionalRelation.OR)
@ConditionalOnClass(MessageUtilDefinition.TYPE_MESSAGE_UTIL)
@ConditionalOnClass(MessageDefinition.TYPE_ACK_CALLBACK)
public class MessageUtilDefinition extends PluginDefinitionAdapter {

    protected static final String TYPE_MESSAGE_UTIL = "org.apache.rocketmq.client.utils.MessageUtil";

    private static final String METHOD_CREATE_REPLY_MESSAGE = "createReplyMessage";

    @Inject
    @InjectLoader(ResourcerType.CORE_IMPL)
    private List<CargoRequire> requires;

    public MessageUtilDefinition() {
        this.matcher = () -> MatcherBuilder.named(TYPE_MESSAGE_UTIL);
        this.interceptors = new InterceptorDefinition[]{
                new InterceptorDefinitionAdapter(
                        MatcherBuilder.named(METHOD_CREATE_REPLY_MESSAGE), () -> new MessageUtilInterceptor(requires))
        };
    }
}
