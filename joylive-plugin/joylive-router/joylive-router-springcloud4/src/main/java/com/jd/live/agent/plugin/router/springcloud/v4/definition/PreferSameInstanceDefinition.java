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
package com.jd.live.agent.plugin.router.springcloud.v4.definition;

import com.jd.live.agent.core.bytekit.matcher.MatcherBuilder;
import com.jd.live.agent.core.extension.annotation.ConditionalOnClass;
import com.jd.live.agent.core.extension.annotation.ConditionalOnProperty;
import com.jd.live.agent.core.extension.annotation.Extension;
import com.jd.live.agent.core.plugin.definition.InterceptorDefinitionAdapter;
import com.jd.live.agent.core.plugin.definition.PluginDefinitionAdapter;
import com.jd.live.agent.governance.config.GovernanceConfig;
import com.jd.live.agent.plugin.router.springcloud.v4.interceptor.PreferSameInstanceInterceptor;

/**
 * PreferSameInstanceDefinition
 *
 * @since 1.0.0
 */
@Extension(value = "PreferSameInstanceDefinition_v3")
@ConditionalOnProperty(name = GovernanceConfig.CONFIG_FLOW_CONTROL_ENABLED, matchIfMissing = true)
@ConditionalOnProperty(name = GovernanceConfig.CONFIG_LIVE_SPRING_ENABLED, matchIfMissing = true)
@ConditionalOnClass(PreferSameInstanceDefinition.TYPE_SAME_INSTANCE_PREFERENCE_SERVICE_INSTANCE_LIST_SUPPLIER)
@ConditionalOnClass(BlockingClusterDefinition.TYPE_HTTP_STATUS_CODE)
public class PreferSameInstanceDefinition extends PluginDefinitionAdapter {

    protected static final String TYPE_SAME_INSTANCE_PREFERENCE_SERVICE_INSTANCE_LIST_SUPPLIER = "org.springframework.cloud.loadbalancer.core.SameInstancePreferenceServiceInstanceListSupplier";

    private static final String METHOD_FILTERED_BY_SAME_INSTANCE_PREFERENCE = "filteredBySameInstancePreference";

    private static final String[] ARGUMENTS_FILTERED_BY_SAME_INSTANCE_PREFERENCE = new String[]{
            "java.util.List"
    };

    public PreferSameInstanceDefinition() {
        super(TYPE_SAME_INSTANCE_PREFERENCE_SERVICE_INSTANCE_LIST_SUPPLIER,
                new InterceptorDefinitionAdapter(
                        MatcherBuilder.named(METHOD_FILTERED_BY_SAME_INSTANCE_PREFERENCE).
                                and(MatcherBuilder.arguments(ARGUMENTS_FILTERED_BY_SAME_INSTANCE_PREFERENCE)),
                        new PreferSameInstanceInterceptor()
                ));
    }
}
