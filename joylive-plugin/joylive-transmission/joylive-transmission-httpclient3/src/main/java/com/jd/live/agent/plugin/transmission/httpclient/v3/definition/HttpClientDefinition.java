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
package com.jd.live.agent.plugin.transmission.httpclient.v3.definition;

import com.jd.live.agent.core.bytekit.matcher.MatcherBuilder;
import com.jd.live.agent.core.extension.annotation.*;
import com.jd.live.agent.core.plugin.definition.InterceptorDefinitionAdapter;
import com.jd.live.agent.core.plugin.definition.PluginDefinition;
import com.jd.live.agent.core.plugin.definition.PluginDefinitionAdapter;
import com.jd.live.agent.governance.config.GovernanceConfig;
import com.jd.live.agent.plugin.transmission.httpclient.v3.interceptor.HttpClientInterceptor;


@Extension(value = "HttpClientDefinition_v3", order = PluginDefinition.ORDER_TRANSMISSION)
@ConditionalOnProperties(value = {
        @ConditionalOnProperty(value = GovernanceConfig.CONFIG_LIVE_ENABLED, matchIfMissing = true),
        @ConditionalOnProperty(value = GovernanceConfig.CONFIG_LANE_ENABLED, matchIfMissing = true),
        @ConditionalOnProperty(value = GovernanceConfig.CONFIG_FLOW_CONTROL_ENABLED, matchIfMissing = true)
}, relation = ConditionalRelation.OR)
@ConditionalOnClass(HttpClientDefinition.TYPE_HTTP_CLIENT)
public class HttpClientDefinition extends PluginDefinitionAdapter {

    public static final String TYPE_HTTP_CLIENT = "org.apache.commons.httpclient.HttpClient";

    private static final String METHOD_EXECUTE_METHOD = "executeMethod";

    private static final String[] ARGUMENTS_EXECUTE_METHOD = new String[]{
            "org.apache.commons.httpclient.HostConfiguration",
            "org.apache.commons.httpclient.HttpMethod",
            "org.apache.commons.httpclient.HttpState"
    };

    public HttpClientDefinition() {
        super(MatcherBuilder.named(TYPE_HTTP_CLIENT),
                new InterceptorDefinitionAdapter(
                        MatcherBuilder.named(METHOD_EXECUTE_METHOD).
                                and(MatcherBuilder.arguments(ARGUMENTS_EXECUTE_METHOD)),
                        new HttpClientInterceptor()));
    }
}
