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
package com.jd.live.agent.plugin.transmission.pulsar.v3.definition;

import com.jd.live.agent.core.bytekit.matcher.MatcherBuilder;
import com.jd.live.agent.core.extension.annotation.ConditionalOnClass;
import com.jd.live.agent.core.extension.annotation.Extension;
import com.jd.live.agent.core.inject.annotation.Inject;
import com.jd.live.agent.core.inject.annotation.Injectable;
import com.jd.live.agent.core.plugin.definition.InterceptorDefinition;
import com.jd.live.agent.core.plugin.definition.InterceptorDefinitionAdapter;
import com.jd.live.agent.core.plugin.definition.PluginDefinition;
import com.jd.live.agent.core.plugin.definition.PluginDefinitionAdapter;
import com.jd.live.agent.governance.annotation.ConditionalOnTransmissionEnabled;
import com.jd.live.agent.governance.invoke.InvocationContext;
import com.jd.live.agent.plugin.transmission.pulsar.v3.interceptor.SendInterceptor;

@Injectable
@Extension(value = "MessageBuilderDefinition_v3", order = PluginDefinition.ORDER_TRANSMISSION)
@ConditionalOnTransmissionEnabled
@ConditionalOnClass(MessageBuilderDefinition.TYPE_TYPED_MESSAGE_BUILDER)
public class MessageBuilderDefinition extends PluginDefinitionAdapter {

    protected static final String TYPE_TYPED_MESSAGE_BUILDER = "org.apache.pulsar.client.api.TypedMessageBuilder";

    private static final String METHOD_SEND = "send";

    private static final String METHOD_SEND_ASYNC = "sendAsync";

    @Inject(InvocationContext.COMPONENT_INVOCATION_CONTEXT)
    private InvocationContext context;

    public MessageBuilderDefinition() {
        this.matcher = () -> MatcherBuilder.isImplement(TYPE_TYPED_MESSAGE_BUILDER);
        this.interceptors = new InterceptorDefinition[]{
                new InterceptorDefinitionAdapter(
                        MatcherBuilder.in(METHOD_SEND, METHOD_SEND_ASYNC),
                        () -> new SendInterceptor(context))};
    }
}
