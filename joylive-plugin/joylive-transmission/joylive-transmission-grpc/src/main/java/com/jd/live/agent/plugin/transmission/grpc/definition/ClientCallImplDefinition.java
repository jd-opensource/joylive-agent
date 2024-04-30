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
package com.jd.live.agent.plugin.transmission.grpc.definition;

import com.jd.live.agent.core.bytekit.matcher.MatcherBuilder;
import com.jd.live.agent.core.extension.annotation.ConditionalOnClass;
import com.jd.live.agent.core.extension.annotation.ConditionalOnProperty;
import com.jd.live.agent.core.extension.annotation.Extension;
import com.jd.live.agent.core.plugin.definition.InterceptorDefinitionAdapter;
import com.jd.live.agent.core.plugin.definition.PluginDefinition;
import com.jd.live.agent.core.plugin.definition.PluginDefinitionAdapter;
import com.jd.live.agent.governance.config.GovernanceConfig;
import com.jd.live.agent.plugin.transmission.grpc.interceptor.ClientCallImplInterceptor;

@Extension(value = "ClientCallImplDefinition", order = PluginDefinition.ORDER_TRANSMISSION)
@ConditionalOnProperty(value = GovernanceConfig.CONFIG_TRANSMISSION_ENABLED, matchIfMissing = true)
@ConditionalOnClass(ClientCallImplDefinition.TYPE_CLIENT_CALL_IMPL)
public class ClientCallImplDefinition extends PluginDefinitionAdapter {

    public static final String TYPE_CLIENT_CALL_IMPL = "io.grpc.internal.ClientCallImpl";

    private static final String METHOD_START = "start";

    private static final String[] ARGUMENTS_START = new String[]{
            "io.grpc.ClientCall.Listener",
            "io.grpc.Metadata"
    };


    public ClientCallImplDefinition() {
        super(MatcherBuilder.named(TYPE_CLIENT_CALL_IMPL),
                new InterceptorDefinitionAdapter(
                        MatcherBuilder.named(METHOD_START).
                                and(MatcherBuilder.arguments(ARGUMENTS_START)),
                        new ClientCallImplInterceptor()));
    }
}
