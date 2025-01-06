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
package com.jd.live.agent.plugin.transmission.springweb.v5.definition;

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
import com.jd.live.agent.governance.context.bag.Propagation;
import com.jd.live.agent.plugin.transmission.springweb.v5.interceptor.HandshakeWebSocketServiceInterceptor;

/**
 * HandshakeWebSocketServiceDefinition
 *
 * @since 1.0.0
 */
@Injectable
@Extension(value = "HandshakeWebSocketServiceDefinition_v5", order = PluginDefinition.ORDER_TRANSMISSION)
@ConditionalOnTransmissionEnabled
@ConditionalOnClass(HandshakeWebSocketServiceDefinition.TYPE_REACTOR_LOADBALANCER)
public class HandshakeWebSocketServiceDefinition extends PluginDefinitionAdapter {

    protected static final String TYPE_REACTOR_LOADBALANCER = "org.springframework.web.reactive.socket.server.support.HandshakeWebSocketService";

    private static final String METHOD_HANDLE_REQUEST = "handleRequest";

    private static final String[] ARGUMENT_HANDLE_REQUEST = new String[]{
            "org.springframework.web.server.ServerWebExchange",
            "org.springframework.web.reactive.socket.WebSocketHandler"
    };

    @Inject(Propagation.COMPONENT_PROPAGATION)
    private Propagation propagation;

    public HandshakeWebSocketServiceDefinition() {
        this.matcher = () -> MatcherBuilder.named(TYPE_REACTOR_LOADBALANCER);
        this.interceptors = new InterceptorDefinition[]{
                new InterceptorDefinitionAdapter(
                        MatcherBuilder.named(METHOD_HANDLE_REQUEST).
                                and(MatcherBuilder.arguments(ARGUMENT_HANDLE_REQUEST)),
                        () -> new HandshakeWebSocketServiceInterceptor(propagation))
        };
    }
}
