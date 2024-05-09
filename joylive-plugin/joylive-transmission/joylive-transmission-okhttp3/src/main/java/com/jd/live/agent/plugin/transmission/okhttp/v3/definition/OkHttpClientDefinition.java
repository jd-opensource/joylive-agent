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
package com.jd.live.agent.plugin.transmission.okhttp.v3.definition;

import com.jd.live.agent.core.bytekit.matcher.MatcherBuilder;
import com.jd.live.agent.core.extension.annotation.*;
import com.jd.live.agent.core.plugin.definition.InterceptorDefinitionAdapter;
import com.jd.live.agent.core.plugin.definition.PluginDefinition;
import com.jd.live.agent.core.plugin.definition.PluginDefinitionAdapter;
import com.jd.live.agent.governance.config.GovernanceConfig;
import com.jd.live.agent.plugin.transmission.okhttp.v3.interceptor.OkHttpClientInterceptor;

@Extension(value = "OkHttpClientDefinition_v3", order = PluginDefinition.ORDER_TRANSMISSION)
@ConditionalOnProperties(value = {
        @ConditionalOnProperty(value = GovernanceConfig.CONFIG_LIVE_ENABLED, matchIfMissing = true),
        @ConditionalOnProperty(value = GovernanceConfig.CONFIG_LANE_ENABLED, matchIfMissing = true)
}, relation = ConditionalRelation.OR)
@ConditionalOnClass(OkHttpClientDefinition.TYPE_OK_HTTPCLIENT)
public class OkHttpClientDefinition extends PluginDefinitionAdapter {

    public static final String TYPE_OK_HTTPCLIENT = "okhttp3.OkHttpClient";

    private static final String METHOD_NEW_CALL = "newCall";

    private static final String[] ARGUMENT_NEW_CALL = new String[]{
            "okhttp3.Request"
    };

    public OkHttpClientDefinition() {
        super(MatcherBuilder.named(TYPE_OK_HTTPCLIENT),
                new InterceptorDefinitionAdapter(
                        MatcherBuilder.named(METHOD_NEW_CALL).
                                and(MatcherBuilder.arguments(ARGUMENT_NEW_CALL)),
                        new OkHttpClientInterceptor()));
    }
}
