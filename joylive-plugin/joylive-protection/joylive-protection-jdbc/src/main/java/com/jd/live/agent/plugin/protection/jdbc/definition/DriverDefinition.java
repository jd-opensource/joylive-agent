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
package com.jd.live.agent.plugin.protection.jdbc.definition;

import com.jd.live.agent.core.bytekit.matcher.MatcherBuilder;
import com.jd.live.agent.core.event.Publisher;
import com.jd.live.agent.core.extension.annotation.ConditionalOnClass;
import com.jd.live.agent.core.extension.annotation.Extension;
import com.jd.live.agent.core.inject.annotation.Inject;
import com.jd.live.agent.core.inject.annotation.Injectable;
import com.jd.live.agent.core.plugin.definition.InterceptorDefinition;
import com.jd.live.agent.core.plugin.definition.InterceptorDefinitionAdapter;
import com.jd.live.agent.core.plugin.definition.PluginDefinition;
import com.jd.live.agent.core.plugin.definition.PluginDefinitionAdapter;
import com.jd.live.agent.governance.annotation.ConditionalOnProtectEnabled;
import com.jd.live.agent.governance.event.DatabaseEvent;
import com.jd.live.agent.governance.policy.PolicySupplier;
import com.jd.live.agent.plugin.protection.jdbc.interceptor.DriverConnectInterceptor;

@Injectable
@Extension(value = "DriverDefinition", order = PluginDefinition.ORDER_PROTECT)
@ConditionalOnProtectEnabled
@ConditionalOnClass(DriverDefinition.TYPE)
public class DriverDefinition extends PluginDefinitionAdapter {

    protected static final String TYPE = "java.sql.Driver";

    private static final String METHOD = "connect";

    private static final String[] ARGUMENTS = new String[]{
            "java.lang.String",
            "java.util.Properties"
    };

    @Inject(PolicySupplier.COMPONENT_POLICY_SUPPLIER)
    private PolicySupplier policySupplier;

    @Inject(Publisher.DATABASE)
    private Publisher<DatabaseEvent> publisher;

    public DriverDefinition() {
        this.matcher = () -> MatcherBuilder.isImplement(TYPE);
        this.interceptors = new InterceptorDefinition[]{
                new InterceptorDefinitionAdapter(
                        MatcherBuilder.named(METHOD).
                                and(MatcherBuilder.arguments(ARGUMENTS)),
                        () -> new DriverConnectInterceptor(policySupplier, publisher)
                )
        };
    }
}
