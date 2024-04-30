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
package com.jd.live.agent.plugin.transimission.nettyhttp.v1.definition;

import com.jd.live.agent.core.bytekit.matcher.MatcherBuilder;
import com.jd.live.agent.core.extension.annotation.ConditionalOnClass;
import com.jd.live.agent.core.extension.annotation.ConditionalOnProperty;
import com.jd.live.agent.core.extension.annotation.Extension;
import com.jd.live.agent.core.plugin.definition.InterceptorDefinitionAdapter;
import com.jd.live.agent.core.plugin.definition.PluginDefinition;
import com.jd.live.agent.core.plugin.definition.PluginDefinitionAdapter;
import com.jd.live.agent.governance.config.GovernanceConfig;
import com.jd.live.agent.plugin.transimission.nettyhttp.v1.interceptor.NettyHttpClientInterceptor;

@Extension(value = "NettyHttpClientDefinition_v1", order = PluginDefinition.ORDER_TRANSMISSION)
@ConditionalOnProperty(value = GovernanceConfig.CONFIG_TRANSMISSION_ENABLED, matchIfMissing = true)
@ConditionalOnClass(NettyHttpClientDefinition.TYPE_HTTP_CLIENT)
public class NettyHttpClientDefinition extends PluginDefinitionAdapter {

    public static final String TYPE_HTTP_CLIENT = "reactor.netty.http.client.HttpClient";

    private static final String METHOD_REQUEST = "request";

    private static final String[] ARGUMENTS_REQUEST = new String[]{
            "io.netty.handler.codec.http.HttpMethod",
    };

    public NettyHttpClientDefinition() {
        super(MatcherBuilder.named(TYPE_HTTP_CLIENT),
                new InterceptorDefinitionAdapter(
                        MatcherBuilder.named(METHOD_REQUEST).
                                and(MatcherBuilder.arguments(ARGUMENTS_REQUEST)),
                        new NettyHttpClientInterceptor()));
    }
}
