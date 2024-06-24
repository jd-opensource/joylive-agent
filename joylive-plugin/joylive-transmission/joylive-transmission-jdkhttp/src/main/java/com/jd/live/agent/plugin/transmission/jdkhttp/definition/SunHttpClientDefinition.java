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
package com.jd.live.agent.plugin.transmission.jdkhttp.definition;

import com.jd.live.agent.core.bytekit.matcher.MatcherBuilder;
import com.jd.live.agent.core.extension.annotation.*;
import com.jd.live.agent.core.plugin.definition.InterceptorDefinitionAdapter;
import com.jd.live.agent.core.plugin.definition.PluginDefinition;
import com.jd.live.agent.core.plugin.definition.PluginDefinitionAdapter;
import com.jd.live.agent.core.plugin.definition.PluginImporter;
import com.jd.live.agent.governance.config.GovernanceConfig;
import com.jd.live.agent.plugin.transmission.jdkhttp.interceptor.SunHttpClientInterceptor;

/**
 * Defines the instrumentation for intercepting the {@code writeRequests} method
 * of the {@code sun.net.www.http.HttpClient} class. This class configures the
 * conditions under which the {@link SunHttpClientInterceptor} is applied, aiming
 * to monitor or modify HTTP request writing behavior.
 *
 * <p>Annotations used:</p>
 * <ul>
 *     <li>{@link Extension} - Marks this class as an extension with a defined purpose
 *     within the framework, allowing it to be automatically discovered and applied.</li>
 *     <li>{@link ConditionalOnProperty} - Ensures that this plugin is active based on the
 *     {@code CONFIG_TRANSMISSION_ENABLED} configuration property.</li>
 *     <li>{@link ConditionalOnClass} - Guarantees that this plugin is only loaded if the
 *     {@code HttpClient} class is available in the runtime environment, preventing class
 *     loading issues in environments with different Java versions or configurations.</li>
 * </ul>
 *
 * @see PluginDefinitionAdapter
 * @see SunHttpClientInterceptor
 */
@Extension(value = "JdkHttpClientDefinition", order = PluginDefinition.ORDER_TRANSMISSION)
@ConditionalOnProperties(value = {
        @ConditionalOnProperty(value = GovernanceConfig.CONFIG_LIVE_ENABLED, matchIfMissing = true),
        @ConditionalOnProperty(value = GovernanceConfig.CONFIG_LANE_ENABLED, matchIfMissing = true),
        @ConditionalOnProperty(value = GovernanceConfig.CONFIG_FLOW_CONTROL_ENABLED, matchIfMissing = true)
}, relation = ConditionalRelation.OR)
@ConditionalOnClass(SunHttpClientDefinition.TYPE_HTTP_CLIENT)
public class SunHttpClientDefinition extends PluginDefinitionAdapter implements PluginImporter {

    public static final String TYPE_HTTP_CLIENT = "sun.net.www.http.HttpClient";

    private static final String METHOD_WRITE_REQUESTS = "writeRequests";

    private static final String[] ARGUMENT_WRITE_REQUESTS = new String[]{
            "sun.net.www.MessageHeader",
            "sun.net.www.http.PosterOutputStream"
    };

    public SunHttpClientDefinition() {
        super(MatcherBuilder.named(TYPE_HTTP_CLIENT),
                new InterceptorDefinitionAdapter(
                        MatcherBuilder.named(METHOD_WRITE_REQUESTS).
                                and(MatcherBuilder.arguments(ARGUMENT_WRITE_REQUESTS)),
                        new SunHttpClientInterceptor()));
    }

    @Override
    public String[] getImports() {
        return new String[]{"sun.net.www.http.HttpClient", "sun.net.www.MessageHeader"};
    }
}
