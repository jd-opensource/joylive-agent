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
package com.jd.live.agent.plugin.router.zuul.definition;

import com.jd.live.agent.core.bytekit.matcher.MatcherBuilder;
import com.jd.live.agent.core.extension.annotation.ConditionalOnClass;
import com.jd.live.agent.core.extension.annotation.Extension;
import com.jd.live.agent.core.plugin.definition.InterceptorDefinition;
import com.jd.live.agent.core.plugin.definition.InterceptorDefinitionAdapter;
import com.jd.live.agent.core.plugin.definition.PluginDefinitionAdapter;
import com.jd.live.agent.plugin.router.zuul.condition.ConditionalOnZuulGovernanceEnabled;
import com.jd.live.agent.plugin.router.zuul.interceptor.GatewayInterceptor;

/**
 * GatewayDefinition
 *
 * @since 1.7.0
 */
@Extension(value = "GatewayDefinition")
@ConditionalOnZuulGovernanceEnabled
@ConditionalOnClass(GatewayDefinition.TYPE)
public class GatewayDefinition extends PluginDefinitionAdapter {

    protected static final String TYPE = "org.springframework.web.servlet.DispatcherServlet";

    private static final String METHOD = "doService";

    public GatewayDefinition() {
        this.matcher = () -> MatcherBuilder.named(TYPE);
        this.interceptors = new InterceptorDefinition[]{
                new InterceptorDefinitionAdapter(MatcherBuilder.named(METHOD),
                        () -> new GatewayInterceptor())
        };
    }
}
