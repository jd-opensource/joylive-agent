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
import com.jd.live.agent.governance.policy.PolicySupplier;
import com.jd.live.agent.plugin.registry.sofarpc.interceptor.ConsumerBootstrapInterceptor;

import java.util.HashMap;
import java.util.Map;

/**
 * ConsumerBootstrapDefinition
 */
@Injectable
@Extension(value = "ConsumerBootstrapDefinition", order = PluginDefinition.ORDER_REGISTRY)
@ConditionalOnGovernanceEnabled
@ConditionalOnClass(ConsumerBootstrapDefinition.TYPE_CONSUMER_BOOTSTRAP)
public class ConsumerBootstrapDefinition extends PluginDefinitionAdapter implements PluginImporter {

    protected static final String TYPE_CONSUMER_BOOTSTRAP = "com.alipay.sofa.rpc.bootstrap.ConsumerBootstrap";

    private static final String METHOD_REFER = "refer";

    @Inject(Application.COMPONENT_APPLICATION)
    private Application application;

    @Inject(PolicySupplier.COMPONENT_POLICY_SUPPLIER)
    private PolicySupplier policySupplier;

    public ConsumerBootstrapDefinition() {
        this.matcher = () -> MatcherBuilder.isSubTypeOf(TYPE_CONSUMER_BOOTSTRAP)
                .and(MatcherBuilder.exists("com.alipay.sofa.rpc.bootstrap.dubbo.DubboConsumerBootstrap", "org.apache.dubbo.config.ReferenceConfig"));
        this.interceptors = new InterceptorDefinition[]{
                new InterceptorDefinitionAdapter(
                        MatcherBuilder.named(METHOD_REFER).
                                and(MatcherBuilder.arguments(0)),
                        () -> new ConsumerBootstrapInterceptor(application, policySupplier))
        };
    }

    @Override
    public Map<String, String> getExports() {
        Map<String, String> exports = new HashMap<>();
        exports.put("java.lang.StackTraceElement", "com.caucho.hessian.io.JavaDeserializer");
        return exports;
    }
}
