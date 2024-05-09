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
package com.jd.live.agent.plugin.router.springcloud.v3.definition;

import com.jd.live.agent.bootstrap.classloader.ResourcerType;
import com.jd.live.agent.core.bytekit.matcher.MatcherBuilder;
import com.jd.live.agent.core.extension.annotation.ConditionalOnClass;
import com.jd.live.agent.core.extension.annotation.ConditionalOnProperty;
import com.jd.live.agent.core.extension.annotation.Extension;
import com.jd.live.agent.core.inject.annotation.Inject;
import com.jd.live.agent.core.inject.annotation.InjectLoader;
import com.jd.live.agent.core.inject.annotation.Injectable;
import com.jd.live.agent.core.plugin.definition.InterceptorDefinition;
import com.jd.live.agent.core.plugin.definition.InterceptorDefinitionAdapter;
import com.jd.live.agent.core.plugin.definition.PluginDefinitionAdapter;
import com.jd.live.agent.governance.config.GovernanceConfig;
import com.jd.live.agent.governance.invoke.InvocationContext;
import com.jd.live.agent.governance.invoke.filter.OutboundFilter;
import com.jd.live.agent.plugin.router.springcloud.v3.interceptor.FeignClientRequestInterceptor;

import java.util.List;

/**
 * FeignRequestDefinition
 *
 * @since 1.0.0
 */
@Injectable
@Extension(value = "InterceptingClientHttpRequestPluginDefinition_v3")
@ConditionalOnProperty(value = GovernanceConfig.CONFIG_LIVE_ENABLED, matchIfMissing = true)
@ConditionalOnProperty(value = GovernanceConfig.CONFIG_LIVE_SPRING_ENABLED, matchIfMissing = true)
@ConditionalOnProperty(value = GovernanceConfig.CONFIG_REGISTRY_ENABLED, matchIfMissing = true)
@ConditionalOnProperty(value = GovernanceConfig.CONFIG_TRANSMISSION_ENABLED, matchIfMissing = true)
@ConditionalOnClass(FeignClientRequestDefinition.TYPE_FEIGN_CLIENT_CLASS)
public class FeignClientRequestDefinition extends PluginDefinitionAdapter {

    public static final String TYPE_FEIGN_CLIENT_CLASS = "feign.Client";

    private static final String METHOD_EXECUTE = "execute";

    private static final String[] ARGUMENT_EXECUTE = new String[]{
            "feign.Request", "feign.Request$Options"
    };

    @Inject(InvocationContext.COMPONENT_INVOCATION_CONTEXT)
    private InvocationContext context;

    @Inject
    @InjectLoader(ResourcerType.PLUGIN)
    private List<OutboundFilter> filters;

    public FeignClientRequestDefinition() {
        this.matcher = () -> MatcherBuilder.isImplement(TYPE_FEIGN_CLIENT_CLASS);
        this.interceptors = new InterceptorDefinition[]{
                new InterceptorDefinitionAdapter(
                        MatcherBuilder.named(METHOD_EXECUTE).
                                and(MatcherBuilder.arguments(ARGUMENT_EXECUTE)),
                        () -> new FeignClientRequestInterceptor(context, filters)
                )
        };
    }
}
