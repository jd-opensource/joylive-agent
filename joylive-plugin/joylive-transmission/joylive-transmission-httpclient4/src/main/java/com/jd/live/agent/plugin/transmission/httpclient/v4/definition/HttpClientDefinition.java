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
package com.jd.live.agent.plugin.transmission.httpclient.v4.definition;

import com.jd.live.agent.core.bytekit.matcher.MatcherBuilder;
import com.jd.live.agent.core.extension.annotation.*;
import com.jd.live.agent.core.plugin.definition.InterceptorDefinitionAdapter;
import com.jd.live.agent.core.plugin.definition.PluginDefinition;
import com.jd.live.agent.core.plugin.definition.PluginDefinitionAdapter;
import com.jd.live.agent.governance.config.GovernanceConfig;
import com.jd.live.agent.plugin.transmission.httpclient.v4.interceptor.HttpClientInterceptor;

@Extension(value = "HttpClientDefinition_v4", order = PluginDefinition.ORDER_TRANSMISSION)
@ConditionalOnProperties(value = {
        @ConditionalOnProperty(value = GovernanceConfig.CONFIG_LIVE_ENABLED, matchIfMissing = true),
        @ConditionalOnProperty(value = GovernanceConfig.CONFIG_LANE_ENABLED, matchIfMissing = true),
        @ConditionalOnProperty(value = GovernanceConfig.CONFIG_FLOW_CONTROL_ENABLED, matchIfMissing = true)
}, relation = ConditionalRelation.OR)
@ConditionalOnClass(HttpClientDefinition.TYPE_ABSTRACT_HTTP_CLIENT)
public class HttpClientDefinition extends PluginDefinitionAdapter {

    protected static final String TYPE_ABSTRACT_HTTP_CLIENT = "org.apache.http.impl.client.AbstractHttpClient";
    private static final String TYPE_DEFAULT_REQUEST_DIRECTOR = "org.apache.http.impl.client.DefaultRequestDirector";
    private static final String TYPE_INTERNAL_HTTP_CLIENT = "org.apache.http.impl.client.InternalHttpClient";
    private static final String TYPE_MINIMAL_HTTP_CLIENT = "org.apache.http.impl.client.MinimalHttpClient";
    private static final String[] TYPE_CLIENTS = {
            TYPE_ABSTRACT_HTTP_CLIENT,
            TYPE_DEFAULT_REQUEST_DIRECTOR,
            TYPE_INTERNAL_HTTP_CLIENT,
            TYPE_MINIMAL_HTTP_CLIENT
    };

    private static final String[] METHOD_EXECUTES = new String[]{"doExecute", "execute"};

    private static final String[] ARGUMENTS_EXECUTE = new String[]{
            "org.apache.http.HttpHost",
            "org.apache.http.HttpRequest",
            "org.apache.http.protocol.HttpContext"
    };

    public HttpClientDefinition() {
        super(MatcherBuilder.in(TYPE_CLIENTS),
                new InterceptorDefinitionAdapter(
                        MatcherBuilder.in(METHOD_EXECUTES).
                                and(MatcherBuilder.arguments(ARGUMENTS_EXECUTE)),
                        new HttpClientInterceptor()));
    }
}
