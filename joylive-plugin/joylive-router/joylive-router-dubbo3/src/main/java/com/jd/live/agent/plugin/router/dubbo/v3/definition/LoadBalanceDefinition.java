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

import com.jd.live.agent.core.bytekit.matcher.MatcherBuilder;
import com.jd.live.agent.core.extension.annotation.ConditionalOnClass;
import com.jd.live.agent.core.extension.annotation.Extension;
import com.jd.live.agent.core.inject.annotation.Inject;
import com.jd.live.agent.core.inject.annotation.Injectable;
import com.jd.live.agent.core.plugin.definition.InterceptorDefinition;
import com.jd.live.agent.core.plugin.definition.InterceptorDefinitionAdapter;
import com.jd.live.agent.core.plugin.definition.PluginDefinitionAdapter;
import com.jd.live.agent.governance.invoke.InvocationContext;
import com.jd.live.agent.plugin.router.dubbo.v3.condition.ConditionalOnDubbo3OnlyRouteEnabled;
import com.jd.live.agent.plugin.router.dubbo.v3.interceptor.LoadBalanceInterceptor;

@Injectable
@Extension(value = "LoadBalanceDefinition_v3")
@ConditionalOnDubbo3OnlyRouteEnabled
@ConditionalOnClass(LoadBalanceDefinition.TYPE_ABSTRACT_CLUSTER)
public class LoadBalanceDefinition extends PluginDefinitionAdapter {

    protected static final String TYPE_ABSTRACT_CLUSTER = "com.alibaba.dubbo.rpc.cluster.support.AbstractClusterInvoker";

    private static final String METHOD_SELECT = "select";

    private static final String[] ARGUMENT_SELECT = new String[]{
            "org.apache.dubbo.rpc.cluster.LoadBalance",
            "org.apache.dubbo.rpc.Invocation",
            "java.util.List",
            "java.util.List"
    };

    @Inject(InvocationContext.COMPONENT_INVOCATION_CONTEXT)
    private InvocationContext context;

    public LoadBalanceDefinition() {
        this.matcher = () -> MatcherBuilder.isSubTypeOf(TYPE_ABSTRACT_CLUSTER)
                .and(MatcherBuilder.not(MatcherBuilder.isAbstract()));
        this.interceptors = new InterceptorDefinition[]{
                new InterceptorDefinitionAdapter(
                        MatcherBuilder.named(METHOD_SELECT)
                                .and(MatcherBuilder.arguments(ARGUMENT_SELECT)),
                        () -> new LoadBalanceInterceptor(context)
                )
        };
    }
}
