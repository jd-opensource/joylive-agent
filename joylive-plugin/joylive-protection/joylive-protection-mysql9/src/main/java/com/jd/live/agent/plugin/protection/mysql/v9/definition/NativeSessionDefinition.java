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
package com.jd.live.agent.plugin.protection.mysql.v9.definition;

import com.jd.live.agent.core.bytekit.matcher.MatcherBuilder;
import com.jd.live.agent.core.extension.annotation.ConditionalOnClass;
import com.jd.live.agent.core.extension.annotation.Extension;
import com.jd.live.agent.core.inject.annotation.Inject;
import com.jd.live.agent.core.inject.annotation.Injectable;
import com.jd.live.agent.core.plugin.definition.InterceptorDefinition;
import com.jd.live.agent.core.plugin.definition.InterceptorDefinitionAdapter;
import com.jd.live.agent.core.plugin.definition.PluginDefinition;
import com.jd.live.agent.core.plugin.definition.PluginDefinitionAdapter;
import com.jd.live.agent.governance.policy.PolicySupplier;
import com.jd.live.agent.plugin.protection.mysql.v9.condition.ConditionalOnMysql9ProtectEnabled;
import com.jd.live.agent.plugin.protection.mysql.v9.interceptor.NativeSessionInterceptor;

@Injectable
@Extension(value = "NativeSessionDefinition_v9", order = PluginDefinition.ORDER_PROTECT)
@ConditionalOnMysql9ProtectEnabled
@ConditionalOnClass(NativeSessionDefinition.TYPE)
public class NativeSessionDefinition extends PluginDefinitionAdapter {

    protected static final String TYPE = "com.mysql.cj.NativeSession";

    private static final String METHOD = "execSQL";

    @Inject(PolicySupplier.COMPONENT_POLICY_SUPPLIER)
    private PolicySupplier policySupplier;

    public NativeSessionDefinition() {
        this.matcher = () -> MatcherBuilder.named(TYPE);
        this.interceptors = new InterceptorDefinition[]{
                new InterceptorDefinitionAdapter(
                        MatcherBuilder.named(METHOD),
                        () -> new NativeSessionInterceptor(policySupplier)
                )
        };
    }
}
