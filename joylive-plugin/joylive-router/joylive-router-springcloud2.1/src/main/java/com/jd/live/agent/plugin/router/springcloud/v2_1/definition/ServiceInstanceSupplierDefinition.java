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
package com.jd.live.agent.plugin.router.springcloud.v2_1.definition;

import com.jd.live.agent.core.bytekit.matcher.MatcherBuilder;
import com.jd.live.agent.core.extension.annotation.ConditionalOnClass;
import com.jd.live.agent.core.extension.annotation.Extension;
import com.jd.live.agent.core.inject.annotation.Inject;
import com.jd.live.agent.core.inject.annotation.Injectable;
import com.jd.live.agent.core.plugin.definition.InterceptorDefinition;
import com.jd.live.agent.core.plugin.definition.InterceptorDefinitionAdapter;
import com.jd.live.agent.core.plugin.definition.PluginDefinitionAdapter;
import com.jd.live.agent.governance.invoke.InvocationContext;
import com.jd.live.agent.plugin.router.springcloud.v2_1.condition.ConditionalOnSpringCloud2OnlyRouteEnabled;
import com.jd.live.agent.plugin.router.springcloud.v2_1.interceptor.ServiceInstanceSupplierInterceptor;

/**
 * ServiceInstanceSupplierDefinition
 *
 * @since 1.7.0
 */
@Injectable
@Extension(value = "ServiceInstanceSupplierDefinition_v2.1")
@ConditionalOnSpringCloud2OnlyRouteEnabled
@ConditionalOnClass(ServiceInstanceSupplierDefinition.TYPE)
public class ServiceInstanceSupplierDefinition extends PluginDefinitionAdapter {

    protected static final String TYPE = "org.springframework.cloud.loadbalancer.core.ServiceInstanceSupplier";

    private static final String METHOD = "get";

    @Inject(InvocationContext.COMPONENT_INVOCATION_CONTEXT)
    private InvocationContext context;

    public ServiceInstanceSupplierDefinition() {
        // enhance default method. so isImplementOf is not used.
        this.matcher = () -> MatcherBuilder.isSubTypeOf(TYPE);
        this.interceptors = new InterceptorDefinition[]{
                new InterceptorDefinitionAdapter(
                        MatcherBuilder.named(METHOD),
                        () -> new ServiceInstanceSupplierInterceptor(context)
                )
        };
    }
}
