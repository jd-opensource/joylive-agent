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
package com.jd.live.agent.plugin.application.springboot.v2.definition;

import com.jd.live.agent.core.bootstrap.AppListener;
import com.jd.live.agent.core.bytekit.matcher.MatcherBuilder;
import com.jd.live.agent.core.extension.annotation.ConditionalOnClass;
import com.jd.live.agent.core.extension.annotation.Extension;
import com.jd.live.agent.core.inject.annotation.Inject;
import com.jd.live.agent.core.inject.annotation.Injectable;
import com.jd.live.agent.core.plugin.definition.InterceptorDefinition;
import com.jd.live.agent.core.plugin.definition.InterceptorDefinitionAdapter;
import com.jd.live.agent.core.plugin.definition.PluginDefinition;
import com.jd.live.agent.core.plugin.definition.PluginDefinitionAdapter;
import com.jd.live.agent.governance.annotation.ConditionalOnGovernanceEnabled;
import com.jd.live.agent.plugin.application.springboot.v2.interceptor.ApplicationCloseInterceptor;

@Injectable
@Extension(value = "SpringApplicationContextDefinition_v5", order = PluginDefinition.ORDER_APPLICATION)
@ConditionalOnGovernanceEnabled
@ConditionalOnClass(SpringApplicationContextDefinition.TYPE_ABSTRACT_APPLICATION_CONTEXT)
public class SpringApplicationContextDefinition extends PluginDefinitionAdapter {

    protected static final String TYPE_ABSTRACT_APPLICATION_CONTEXT = "org.springframework.context.support.AbstractApplicationContext";

    private static final String METHOD_STOP = "doClose";

    @Inject(value = AppListener.COMPONENT_APPLICATION_LISTENER, component = true)
    private AppListener listener;

    public SpringApplicationContextDefinition() {
        this.matcher = () -> MatcherBuilder.named(TYPE_ABSTRACT_APPLICATION_CONTEXT);
        this.interceptors = new InterceptorDefinition[]{
                new InterceptorDefinitionAdapter(MatcherBuilder.named(METHOD_STOP), () -> new ApplicationCloseInterceptor(listener))
        };
    }
}
