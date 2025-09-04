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
package com.jd.live.agent.plugin.registry.nacos.v2_0.definition;

import com.jd.live.agent.core.bytekit.matcher.MatcherBuilder;
import com.jd.live.agent.core.extension.annotation.ConditionalOnClass;
import com.jd.live.agent.core.extension.annotation.Extension;
import com.jd.live.agent.core.inject.annotation.Inject;
import com.jd.live.agent.core.inject.annotation.Injectable;
import com.jd.live.agent.core.instance.Application;
import com.jd.live.agent.core.plugin.definition.InterceptorDefinition;
import com.jd.live.agent.core.plugin.definition.InterceptorDefinitionAdapter;
import com.jd.live.agent.core.plugin.definition.PluginDefinition;
import com.jd.live.agent.core.plugin.definition.PluginDefinitionAdapter;
import com.jd.live.agent.plugin.registry.nacos.v2_0.condition.ConditionalOnNacos20GovernanceEnabled;
import com.jd.live.agent.plugin.registry.nacos.v2_0.interceptor.NacosBeatInfoInterceptor;

/**
 * NacosBeatInfoDefinition
 */
@Injectable
@Extension(value = "NacosBeatInfoDefinition_v2.0", order = PluginDefinition.ORDER_REGISTRY)
@ConditionalOnNacos20GovernanceEnabled
@ConditionalOnClass(NacosBeatInfoDefinition.TYPE)
public class NacosBeatInfoDefinition extends PluginDefinitionAdapter {

    protected static final String TYPE = "com.alibaba.nacos.client.naming.beat.BeatInfo";

    private static final String METHOD = "isStopped";

    @Inject(Application.COMPONENT_APPLICATION)
    private Application application;

    public NacosBeatInfoDefinition() {
        this.matcher = () -> MatcherBuilder.named(TYPE);
        this.interceptors = new InterceptorDefinition[]{
                new InterceptorDefinitionAdapter(
                        MatcherBuilder.named(METHOD), () -> new NacosBeatInfoInterceptor(application)),
        };
    }
}
