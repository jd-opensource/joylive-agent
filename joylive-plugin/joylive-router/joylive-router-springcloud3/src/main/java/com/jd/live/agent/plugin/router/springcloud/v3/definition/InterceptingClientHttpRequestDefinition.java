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
import com.jd.live.agent.plugin.router.springcloud.v3.interceptor.InterceptingClientHttpRequestInterceptor;

import java.util.List;

/**
 * InterceptingClientHttpRequestDefinition
 *
 * @author Zhiguo.Chen
 * @since 1.0.0
 */
@Injectable
@Extension(value = "InterceptingClientHttpRequestPluginDefinition_v3")
@ConditionalOnProperty(value = GovernanceConfig.CONFIG_LIVE_ENABLED, matchIfMissing = true)
@ConditionalOnProperty(value = GovernanceConfig.CONFIG_LIVE_SPRING_ENABLED, matchIfMissing = true)
@ConditionalOnProperty(value = GovernanceConfig.CONFIG_REGISTRY_ENABLED, matchIfMissing = true)
@ConditionalOnProperty(value = GovernanceConfig.CONFIG_TRANSMISSION_ENABLED, matchIfMissing = true)
@ConditionalOnClass(InterceptingClientHttpRequestDefinition.TYPE_INTERCEPTING_CLIENT_HTTP_REQUEST)
public class InterceptingClientHttpRequestDefinition extends PluginDefinitionAdapter {

    protected static final String TYPE_INTERCEPTING_CLIENT_HTTP_REQUEST = "org.springframework.http.client.InterceptingClientHttpRequest";

    private static final String METHOD_EXECUTE_INTERNAL = "executeInternal";

    private static final String[] ARGUMENT_EXECUTE_INTERNAL = new String[]{
            "org.springframework.http.HttpHeaders"
    };

    @Inject(InvocationContext.COMPONENT_INVOCATION_CONTEXT)
    private InvocationContext context;

    @Inject
    @InjectLoader(ResourcerType.PLUGIN)
    private List<OutboundFilter> filters;

    public InterceptingClientHttpRequestDefinition() {
        this.matcher = () -> MatcherBuilder.named(TYPE_INTERCEPTING_CLIENT_HTTP_REQUEST);
        this.interceptors = new InterceptorDefinition[]{
                new InterceptorDefinitionAdapter(
                        MatcherBuilder.named(METHOD_EXECUTE_INTERNAL).
                                and(MatcherBuilder.arguments(ARGUMENT_EXECUTE_INTERNAL)),
                        () -> new InterceptingClientHttpRequestInterceptor(context, filters)
                )
        };
    }
}
