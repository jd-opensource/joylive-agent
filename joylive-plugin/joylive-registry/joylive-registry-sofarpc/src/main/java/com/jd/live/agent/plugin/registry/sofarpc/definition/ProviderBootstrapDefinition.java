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
import com.jd.live.agent.core.plugin.definition.*;
import com.jd.live.agent.governance.annotation.ConditionalOnGovernanceEnabled;
import com.jd.live.agent.governance.registry.Registry;
import com.jd.live.agent.plugin.registry.sofarpc.interceptor.ProviderBootstrapInterceptor;

import java.util.HashMap;
import java.util.Map;

/**
 * ProviderBootstrapDefinition
 */
@Injectable
@Extension(value = "ProviderBootstrapDefinition", order = PluginDefinition.ORDER_REGISTRY)
@ConditionalOnGovernanceEnabled
@ConditionalOnClass(ProviderBootstrapDefinition.TYPE_PROVIDER_BOOTSTRAP)
public class ProviderBootstrapDefinition extends PluginDefinitionAdapter implements PluginImporter {

    protected static final String TYPE_PROVIDER_BOOTSTRAP = "com.alipay.sofa.rpc.bootstrap.ProviderBootstrap";

    private static final String METHOD_EXPORT = "export";

    @Inject(Application.COMPONENT_APPLICATION)
    private Application application;

    @Inject(Registry.COMPONENT_REGISTRY)
    private Registry registry;

    public ProviderBootstrapDefinition() {
        this.matcher = () -> MatcherBuilder.isSubTypeOf(TYPE_PROVIDER_BOOTSTRAP).
                and(MatcherBuilder.exists("com.alipay.sofa.rpc.bootstrap.dubbo.DubboProviderBootstrap", "org.apache.dubbo.config.ServiceConfig"));
        this.interceptors = new InterceptorDefinition[]{
                new InterceptorDefinitionAdapter(
                        MatcherBuilder.named(METHOD_EXPORT).
                                and(MatcherBuilder.arguments(0)),
                        () -> new ProviderBootstrapInterceptor(application, registry))
        };
    }

    @Override
    public Map<String, String> getExports() {
        Map<String, String> exports = new HashMap<>();
        exports.put("java.lang.StackTraceElement", TYPE_MODULE);
        return exports;
    }
}
