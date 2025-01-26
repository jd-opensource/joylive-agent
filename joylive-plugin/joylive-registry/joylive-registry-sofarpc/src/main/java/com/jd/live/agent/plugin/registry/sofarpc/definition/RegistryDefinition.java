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
package com.jd.live.agent.plugin.registry.sofarpc.definition;

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
import com.jd.live.agent.governance.annotation.ConditionalOnGovernanceEnabled;
import com.jd.live.agent.governance.registry.Registry;
import com.jd.live.agent.plugin.registry.sofarpc.interceptor.RegistryInterceptor;

import java.util.*;

/**
 * RegistryDefinition
 */
@Injectable
@Extension(value = "RegistryDefinition_v3", order = PluginDefinition.ORDER_REGISTRY)
@ConditionalOnGovernanceEnabled
@ConditionalOnClass(RegistryDefinition.TYPE_REGISTRY)
public class RegistryDefinition extends PluginDefinitionAdapter {

    protected static final String TYPE_REGISTRY = "com.alipay.sofa.rpc.registry.Registry";

    private static final String METHOD_REGISTER = "register";

    private static final String[] ARGUMENT_REGISTER = new String[]{
            "com.alipay.sofa.rpc.config.ProviderConfig"
    };

    @Inject(Application.COMPONENT_APPLICATION)
    private Application application;

    @Inject(Registry.COMPONENT_REGISTRY)
    private Registry registry;

    public RegistryDefinition() {
        Map<String, Set<String>> conditions = new HashMap<>();
        conditions.computeIfAbsent("com.alipay.sofa.rpc.registry.zk.ZookeeperRegistry", s -> new HashSet<>())
                .addAll(Arrays.asList("org.apache.curator.RetryPolicy", "org.apache.zookeeper.CreateMode"));
        conditions.computeIfAbsent("com.alipay.sofa.rpc.registry.sofa.SofaRegistry", s -> new HashSet<>())
                .add("com.alipay.sofa.registry.client.api.Subscriber");
        conditions.computeIfAbsent("com.alipay.sofa.rpc.registry.polaris.PolarisRegistry", s -> new HashSet<>())
                .add("com.tencent.polaris.api.core.ProviderAPI");
        conditions.computeIfAbsent("com.alipay.sofa.rpc.registry.nacos.NacosRegistry", s -> new HashSet<>())
                .add("com.alibaba.nacos.api.naming.NamingService");
        conditions.computeIfAbsent("com.alipay.sofa.rpc.registry.kubernetes.KubernetesRegistry", s -> new HashSet<>())
                .add("io.fabric8.kubernetes.api.model.Pod");
        this.matcher = () -> MatcherBuilder.isSubTypeOf(TYPE_REGISTRY).and(MatcherBuilder.exists(conditions));
        this.interceptors = new InterceptorDefinition[]{
                new InterceptorDefinitionAdapter(
                        MatcherBuilder.named(METHOD_REGISTER)
                                .and(MatcherBuilder.arguments(ARGUMENT_REGISTER))
                                .and(MatcherBuilder.not(MatcherBuilder.isAbstract())),
                        () -> new RegistryInterceptor(application, registry))
        };
    }
}
