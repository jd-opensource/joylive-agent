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
package com.jd.live.agent.plugin.transmission.thread.definition;

import com.jd.live.agent.core.bytekit.matcher.MatcherBuilder;
import com.jd.live.agent.core.extension.annotation.ConditionalOnProperties;
import com.jd.live.agent.core.extension.annotation.ConditionalOnProperty;
import com.jd.live.agent.core.extension.annotation.ConditionalRelation;
import com.jd.live.agent.core.extension.annotation.Extension;
import com.jd.live.agent.core.inject.annotation.Inject;
import com.jd.live.agent.core.inject.annotation.Injectable;
import com.jd.live.agent.core.plugin.definition.*;
import com.jd.live.agent.core.thread.Camera;
import com.jd.live.agent.governance.config.GovernanceConfig;
import com.jd.live.agent.plugin.transmission.thread.interceptor.ExecutorInterceptor;

import java.util.List;

/**
 * ScheduledExecutorServiceDefinition
 */
@Injectable
@Extension(value = "ScheduledExecutorServiceDefinition", order = PluginDefinition.ORDER_TRANSMISSION)
@ConditionalOnProperties(value = {
        @ConditionalOnProperty(value = GovernanceConfig.CONFIG_LIVE_ENABLED, matchIfMissing = true),
        @ConditionalOnProperty(value = GovernanceConfig.CONFIG_LANE_ENABLED, matchIfMissing = true),
        @ConditionalOnProperty(value = GovernanceConfig.CONFIG_FLOW_CONTROL_ENABLED, matchIfMissing = true)
}, relation = ConditionalRelation.OR)
@ConditionalOnProperty(value = GovernanceConfig.CONFIG_TRANSMISSION_THREADPOOL_ENABLED)
public class ScheduledExecutorServiceDefinition extends PluginDefinitionAdapter implements PluginImporter {
    private static final String TYPE_SCHEDULED_EXECUTOR_SERVICE = "java.util.concurrent.ScheduledExecutorService";

    private static final String METHOD_SCHEDULE = "schedule";

    private static final String METHOD_SCHEDULE_AT_FIXED_RATE = "scheduleAtFixedRate";

    private static final String METHOD_SCHEDULE_WITH_FIXED_DELAY = "scheduleWithFixedDelay";

    private static final String[] METHODS = {METHOD_SCHEDULE, METHOD_SCHEDULE_AT_FIXED_RATE, METHOD_SCHEDULE_WITH_FIXED_DELAY};

    @Inject
    private List<Camera> handlers;

    @Inject(GovernanceConfig.COMPONENT_GOVERNANCE_CONFIG)
    private GovernanceConfig governanceConfig;

    public ScheduledExecutorServiceDefinition() {
        this.matcher = () -> MatcherBuilder.isImplement(TYPE_SCHEDULED_EXECUTOR_SERVICE).
                and(MatcherBuilder.not(MatcherBuilder.in(governanceConfig.getTransmitConfig().getThreadConfig().getExcludeExecutors())));
        this.interceptors = new InterceptorDefinition[]{
                new InterceptorDefinitionAdapter(MatcherBuilder.in(METHODS).and(MatcherBuilder.isPublic()),
                        () -> new ExecutorInterceptor(handlers, governanceConfig.getTransmitConfig().getThreadConfig()))};
    }

    @Override
    public String[] getImports() {
        return new String[]{"java.util.concurrent.FutureTask"};
    }

}