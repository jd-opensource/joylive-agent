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
package com.jd.live.agent.plugin.transmission.sofarpc.definition;

import com.jd.live.agent.core.bytekit.matcher.MatcherBuilder;
import com.jd.live.agent.core.extension.annotation.ConditionalOnClass;
import com.jd.live.agent.core.extension.annotation.Extension;
import com.jd.live.agent.core.inject.annotation.Inject;
import com.jd.live.agent.core.inject.annotation.Injectable;
import com.jd.live.agent.core.plugin.definition.InterceptorDefinition;
import com.jd.live.agent.core.plugin.definition.InterceptorDefinitionAdapter;
import com.jd.live.agent.core.plugin.definition.PluginDefinition;
import com.jd.live.agent.core.plugin.definition.PluginDefinitionAdapter;
import com.jd.live.agent.governance.annotation.ConditionalOnTransmissionEnabled;
import com.jd.live.agent.governance.context.bag.Propagation;
import com.jd.live.agent.plugin.transmission.sofarpc.interceptor.SofaRpcClientInterceptor;

@Injectable
@Extension(value = "SofaRpcClientDefinition", order = PluginDefinition.ORDER_TRANSMISSION)
@ConditionalOnTransmissionEnabled
@ConditionalOnClass(SofaRpcClientDefinition.TYPE_ABSTRACT_CLUSTER)
public class SofaRpcClientDefinition extends PluginDefinitionAdapter {

    public static final String TYPE_ABSTRACT_CLUSTER = "com.alipay.sofa.rpc.client.AbstractCluster";

    private static final String METHOD_INVOKE = "invoke";

    protected static final String[] ARGUMENT_INVOKE = new String[]{
            "com.alipay.sofa.rpc.core.request.SofaRequest"
    };

    @Inject(Propagation.COMPONENT_PROPAGATION)
    private Propagation propagation;

    public SofaRpcClientDefinition() {
        this.matcher = () -> MatcherBuilder.named(TYPE_ABSTRACT_CLUSTER);
        this.interceptors = new InterceptorDefinition[]{
                new InterceptorDefinitionAdapter(
                        MatcherBuilder.named(METHOD_INVOKE).
                                and(MatcherBuilder.arguments(ARGUMENT_INVOKE)),
                        () -> new SofaRpcClientInterceptor(propagation))};
    }
}
