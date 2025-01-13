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
package com.jd.live.agent.plugin.registry.grpc.definition;

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
import com.jd.live.agent.governance.policy.PolicySupplier;
import com.jd.live.agent.plugin.registry.grpc.interceptor.DiscoveryClientConstructorInterceptor;

/**
 * DiscoveryClientNameResolverDefinition
 */
@Injectable
@Extension(value = "DiscoveryClientNameResolverDefinition", order = PluginDefinition.ORDER_REGISTRY)
@ConditionalOnGovernanceEnabled
@ConditionalOnClass(DiscoveryClientNameResolverDefinition.TYPE_DISCOVERY_CLIENT_NAME_RESOLVER)
public class DiscoveryClientNameResolverDefinition extends PluginDefinitionAdapter {

    protected static final String TYPE_DISCOVERY_CLIENT_NAME_RESOLVER = "net.devh.boot.grpc.client.nameresolver.DiscoveryClientNameResolver";

    @Inject(PolicySupplier.COMPONENT_POLICY_SUPPLIER)
    private PolicySupplier policySupplier;

    public DiscoveryClientNameResolverDefinition() {
        this.matcher = () -> MatcherBuilder.named(TYPE_DISCOVERY_CLIENT_NAME_RESOLVER);
        this.interceptors = new InterceptorDefinition[]{
                new InterceptorDefinitionAdapter(
                        MatcherBuilder.isConstructor(), () -> new DiscoveryClientConstructorInterceptor(policySupplier)),
        };
    }
}
