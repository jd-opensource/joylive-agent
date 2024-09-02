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
import com.jd.live.agent.core.extension.annotation.*;
import com.jd.live.agent.core.inject.annotation.Injectable;
import com.jd.live.agent.core.plugin.definition.InterceptorDefinition;
import com.jd.live.agent.core.plugin.definition.InterceptorDefinitionAdapter;
import com.jd.live.agent.core.plugin.definition.PluginDefinition;
import com.jd.live.agent.core.plugin.definition.PluginDefinitionAdapter;
import com.jd.live.agent.governance.config.GovernanceConfig;
import com.jd.live.agent.plugin.transmission.springweb.v5.interceptor.ClientHttpRequestFactoryInterceptor;

/**
 * ClientHttpRequestFactoryDefinition
 *
 * @author Zhiguo.Chen
 * @since 1.0.0
 */
@Injectable
@Extension(value = "ClientHttpRequestFactoryDefinition_v5", order = PluginDefinition.ORDER_TRANSMISSION)
@ConditionalOnProperties(value = {
        @ConditionalOnProperty(value = GovernanceConfig.CONFIG_LIVE_ENABLED, matchIfMissing = true),
        @ConditionalOnProperty(value = GovernanceConfig.CONFIG_LANE_ENABLED, matchIfMissing = true),
        @ConditionalOnProperty(value = GovernanceConfig.CONFIG_FLOW_CONTROL_ENABLED, matchIfMissing = true)
}, relation = ConditionalRelation.OR)
@ConditionalOnClass(ClientHttpRequestFactoryDefinition.TYPE_CLIENT_HTTP_REQUEST_FACTORY)
public class ClientHttpRequestFactoryDefinition extends PluginDefinitionAdapter {

    protected static final String TYPE_CLIENT_HTTP_REQUEST_FACTORY = "org.springframework.http.client.ClientHttpRequestFactory";

    private static final String METHOD_CREATE_REQUEST = "createRequest";

    private static final String[] ARGUMENT_HANDLE = new String[]{
            "java.net.URI", "org.springframework.http.HttpMethod"
    };

    public ClientHttpRequestFactoryDefinition() {
        this.matcher = () -> MatcherBuilder.isImplement(TYPE_CLIENT_HTTP_REQUEST_FACTORY);
        this.interceptors = new InterceptorDefinition[]{
                new InterceptorDefinitionAdapter(
                        MatcherBuilder.named(METHOD_CREATE_REQUEST).
                                and(MatcherBuilder.arguments(ARGUMENT_HANDLE)),
                        new ClientHttpRequestFactoryInterceptor())};
    }
}
