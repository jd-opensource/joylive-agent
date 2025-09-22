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
package com.jd.live.agent.plugin.router.springcloud.v5.definition;

import com.jd.live.agent.core.bytekit.matcher.MatcherBuilder;
import com.jd.live.agent.core.extension.annotation.ConditionalOnClass;
import com.jd.live.agent.core.extension.annotation.Extension;
import com.jd.live.agent.core.inject.annotation.Inject;
import com.jd.live.agent.core.inject.annotation.Injectable;
import com.jd.live.agent.core.plugin.definition.InterceptorDefinition;
import com.jd.live.agent.core.plugin.definition.InterceptorDefinitionAdapter;
import com.jd.live.agent.core.plugin.definition.PluginDefinitionAdapter;
import com.jd.live.agent.governance.invoke.InvocationContext;
import com.jd.live.agent.plugin.router.springcloud.v5.condition.ConditionalOnSpringWeb7GovernanceEnabled;
import com.jd.live.agent.plugin.router.springcloud.v5.interceptor.FeignClientInterceptor;

/**
 * FeignClientDefinition
 */
@Extension(value = "FeignClientDefinition_v6")
@ConditionalOnSpringWeb7GovernanceEnabled
@ConditionalOnClass(FeignClientDefinition.TYPE)
@Injectable
public class FeignClientDefinition extends PluginDefinitionAdapter {

    protected static final String TYPE = "feign.Client";

    private static final String METHOD = "execute";

    private static final String[] ARGUMENT = new String[]{
            "feign.Request",
            "feign.Request$Options"
    };

    @Inject(InvocationContext.COMPONENT_INVOCATION_CONTEXT)
    private InvocationContext context;

    public FeignClientDefinition() {
        this.matcher = () -> MatcherBuilder.isImplement(TYPE);
        this.interceptors = new InterceptorDefinition[]{
                new InterceptorDefinitionAdapter(MatcherBuilder.named(METHOD).and(MatcherBuilder.arguments(ARGUMENT)),
                        () -> new FeignClientInterceptor(context))
        };
    }
}
