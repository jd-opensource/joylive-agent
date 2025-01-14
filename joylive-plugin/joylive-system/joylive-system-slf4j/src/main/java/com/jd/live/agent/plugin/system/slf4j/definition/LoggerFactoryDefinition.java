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
package com.jd.live.agent.plugin.system.slf4j.definition;

import com.jd.live.agent.core.bytekit.matcher.MatcherBuilder;
import com.jd.live.agent.core.config.ConfigCenter;
import com.jd.live.agent.core.extension.annotation.ConditionalOnClass;
import com.jd.live.agent.core.extension.annotation.Extension;
import com.jd.live.agent.core.inject.annotation.Inject;
import com.jd.live.agent.core.inject.annotation.Injectable;
import com.jd.live.agent.core.plugin.definition.InterceptorDefinition;
import com.jd.live.agent.core.plugin.definition.InterceptorDefinitionAdapter;
import com.jd.live.agent.core.plugin.definition.PluginDefinition;
import com.jd.live.agent.core.plugin.definition.PluginDefinitionAdapter;
import com.jd.live.agent.governance.annotation.ConditionalOnConfigCenterEnabled;
import com.jd.live.agent.governance.config.GovernanceConfig;
import com.jd.live.agent.plugin.system.slf4j.interceptor.LoggerFactoryInterceptor;

@Injectable
@Extension(value = "LoggerFactoryDefinition", order = PluginDefinition.ORDER_SYSTEM)
@ConditionalOnConfigCenterEnabled
@ConditionalOnClass(LoggerFactoryDefinition.TYPE)
public class LoggerFactoryDefinition extends PluginDefinitionAdapter {

    protected static final String TYPE = "org.slf4j.LoggerFactory";

    private static final String METHOD = "getLogger";

    private static final String[] ARGUMENT = new String[]{
            "java.lang.String",
    };

    @Inject(ConfigCenter.COMPONENT_CONFIG_CENTER)
    private ConfigCenter configCenter;

    @Inject(GovernanceConfig.COMPONENT_GOVERNANCE_CONFIG)
    private GovernanceConfig governanceConfig;

    public LoggerFactoryDefinition() {
        this.matcher = () -> MatcherBuilder.named(TYPE);
        this.interceptors = new InterceptorDefinition[]{
                new InterceptorDefinitionAdapter(
                        MatcherBuilder.named(METHOD).
                                and(MatcherBuilder.arguments(ARGUMENT)),
                        () -> new LoggerFactoryInterceptor(configCenter, governanceConfig))
        };
    }
}
