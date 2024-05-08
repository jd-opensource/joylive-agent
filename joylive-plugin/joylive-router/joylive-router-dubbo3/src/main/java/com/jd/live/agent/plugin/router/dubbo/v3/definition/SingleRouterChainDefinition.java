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
package com.jd.live.agent.plugin.router.dubbo.v3.definition;

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
import com.jd.live.agent.governance.invoke.filter.RouteFilter;
import com.jd.live.agent.plugin.router.dubbo.v3.interceptor.SingleRouterChainInterceptor;

import java.util.List;

@Injectable
@Extension(value = "SingleRouterChainDefinition_v3")
@ConditionalOnProperty(value = GovernanceConfig.CONFIG_LIVE_ENABLED, matchIfMissing = true)
@ConditionalOnProperty(value = GovernanceConfig.CONFIG_LIVE_DUBBO_ENABLED, matchIfMissing = true)
@ConditionalOnProperty(value = GovernanceConfig.CONFIG_REGISTRY_ENABLED, matchIfMissing = true)
@ConditionalOnProperty(value = GovernanceConfig.CONFIG_TRANSMISSION_ENABLED, matchIfMissing = true)
@ConditionalOnClass(SingleRouterChainDefinition.TYPE_SINGLE_ROUTER_CHAIN)
public class SingleRouterChainDefinition extends PluginDefinitionAdapter {

    protected static final String TYPE_SINGLE_ROUTER_CHAIN = "org.apache.dubbo.rpc.cluster.SingleRouterChain";

    private static final String METHOD_ROUTE = "route";

    private static final String[] ARGUMENT_ROUTE = new String[]{
            "org.apache.dubbo.common.URL",
            "org.apache.dubbo.rpc.cluster.router.state.BitList",
            "org.apache.dubbo.rpc.Invocation"
    };

    @Inject(InvocationContext.COMPONENT_INVOCATION_CONTEXT)
    private InvocationContext context;

    @Inject
    @InjectLoader(ResourcerType.PLUGIN)
    private List<RouteFilter> routeFilters;

    public SingleRouterChainDefinition() {
        this.matcher = () -> MatcherBuilder.named(TYPE_SINGLE_ROUTER_CHAIN);
        this.interceptors = new InterceptorDefinition[]{
                new InterceptorDefinitionAdapter(
                        MatcherBuilder.named(METHOD_ROUTE).
                                and(MatcherBuilder.arguments(ARGUMENT_ROUTE)),
                        () -> new SingleRouterChainInterceptor(context, routeFilters)
                )
        };
    }
}
