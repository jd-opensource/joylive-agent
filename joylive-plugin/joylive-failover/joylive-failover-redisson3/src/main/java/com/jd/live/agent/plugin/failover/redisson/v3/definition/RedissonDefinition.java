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
package com.jd.live.agent.plugin.failover.redisson.v3.definition;

import com.jd.live.agent.core.bytekit.matcher.MatcherBuilder;
import com.jd.live.agent.core.extension.annotation.ConditionalOnClass;
import com.jd.live.agent.core.extension.annotation.Extension;
import com.jd.live.agent.core.inject.annotation.Inject;
import com.jd.live.agent.core.inject.annotation.Injectable;
import com.jd.live.agent.core.plugin.definition.InterceptorDefinition;
import com.jd.live.agent.core.plugin.definition.InterceptorDefinitionAdapter;
import com.jd.live.agent.core.plugin.definition.PluginDefinition;
import com.jd.live.agent.core.plugin.definition.PluginDefinitionAdapter;
import com.jd.live.agent.governance.invoke.InvocationContext;
import com.jd.live.agent.plugin.failover.redisson.v3.condition.ConditionalOnFailoverRedisson3Enabled;
import com.jd.live.agent.plugin.failover.redisson.v3.interceptor.RedissonInterceptor;

@Injectable
@Extension(value = "RedissonDefinition_v3", order = PluginDefinition.ORDER_FAILOVER)
@ConditionalOnFailoverRedisson3Enabled
@ConditionalOnClass(RedissonDefinition.TYPE)
public class RedissonDefinition extends PluginDefinitionAdapter {

    protected static final String TYPE = "org.redisson.Redisson";

    private static final String[] ARGUMENTS = new String[]{
            "org.redisson.config.Config"
    };

    @Inject(InvocationContext.COMPONENT_INVOCATION_CONTEXT)
    private InvocationContext context;

    public RedissonDefinition() {
        this.matcher = () -> MatcherBuilder.named(TYPE);
        this.interceptors = new InterceptorDefinition[]{
                new InterceptorDefinitionAdapter(
                        MatcherBuilder.isConstructor().and(MatcherBuilder.arguments(ARGUMENTS)),
                        () -> new RedissonInterceptor(context)),
        };
    }
}
