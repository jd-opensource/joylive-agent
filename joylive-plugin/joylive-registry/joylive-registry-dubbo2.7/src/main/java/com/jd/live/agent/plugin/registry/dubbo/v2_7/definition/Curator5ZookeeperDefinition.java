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
package com.jd.live.agent.plugin.registry.dubbo.v2_7.definition;

import com.jd.live.agent.core.bytekit.matcher.MatcherBuilder;
import com.jd.live.agent.core.extension.annotation.ConditionalOnClass;
import com.jd.live.agent.core.extension.annotation.Extension;
import com.jd.live.agent.core.inject.annotation.Inject;
import com.jd.live.agent.core.inject.annotation.Injectable;
import com.jd.live.agent.core.plugin.definition.InterceptorDefinition;
import com.jd.live.agent.core.plugin.definition.InterceptorDefinitionAdapter;
import com.jd.live.agent.core.plugin.definition.PluginDefinition;
import com.jd.live.agent.core.plugin.definition.PluginDefinitionAdapter;
import com.jd.live.agent.core.util.time.Timer;
import com.jd.live.agent.governance.probe.HealthProbe;
import com.jd.live.agent.plugin.registry.dubbo.v2_7.condition.ConditionalOnDubbo27GovernanceEnabled;
import com.jd.live.agent.plugin.registry.dubbo.v2_7.interceptor.Curator5ZookeeperInterceptor;

/**
 * CuratorZookeeperDefinition
 */
@Injectable
@Extension(value = "CuratorZookeeperDefinition_v2.7", order = PluginDefinition.ORDER_REGISTRY)
@ConditionalOnDubbo27GovernanceEnabled
@ConditionalOnClass(Curator5ZookeeperDefinition.TYPE)
public class Curator5ZookeeperDefinition extends PluginDefinitionAdapter {

    protected static final String TYPE = "org.apache.dubbo.remoting.zookeeper.curator5.Curator5ZookeeperTransporter";

    private static final String METHOD = "createZookeeperClient";

    private static final String[] ARGUMENT = new String[]{
            "org.apache.dubbo.common.URL"
    };

    @Inject(Timer.COMPONENT_TIMER)
    private Timer timer;

    @Inject(HealthProbe.ZOOKEEPER)
    private HealthProbe probe;

    public Curator5ZookeeperDefinition() {
        this.matcher = () -> MatcherBuilder.isSubTypeOf(TYPE);
        this.interceptors = new InterceptorDefinition[]{
                new InterceptorDefinitionAdapter(
                        MatcherBuilder.named(METHOD).and(MatcherBuilder.arguments(ARGUMENT)),
                        () -> new Curator5ZookeeperInterceptor(timer, probe))
        };
    }
}
