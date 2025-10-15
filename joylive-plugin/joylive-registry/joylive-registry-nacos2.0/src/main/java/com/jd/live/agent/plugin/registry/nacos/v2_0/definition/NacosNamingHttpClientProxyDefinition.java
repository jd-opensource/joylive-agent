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
package com.jd.live.agent.plugin.registry.nacos.v2_0.definition;

import com.jd.live.agent.core.bytekit.matcher.MatcherBuilder;
import com.jd.live.agent.core.extension.annotation.ConditionalOnClass;
import com.jd.live.agent.core.extension.annotation.Extension;
import com.jd.live.agent.core.inject.annotation.Injectable;
import com.jd.live.agent.core.plugin.definition.InterceptorDefinition;
import com.jd.live.agent.core.plugin.definition.InterceptorDefinitionAdapter;
import com.jd.live.agent.core.plugin.definition.PluginDefinition;
import com.jd.live.agent.core.plugin.definition.PluginDefinitionAdapter;
import com.jd.live.agent.plugin.registry.nacos.v2_0.condition.ConditionalOnNacos20Enabled;
import com.jd.live.agent.plugin.registry.nacos.v2_0.interceptor.NacosNamingHttpClientProxyInterceptor;

/**
 * Nacos Naming Client Proxy Plugin Definition.
 *
 * <p>This plugin is used to fix the issue where clients cannot immediately
 * recognize token changes after the server modifies the authentication token.
 * It intercepts the NamingGrpcClientProxy to handle token refresh scenarios.</p>
 *
 * <p>The plugin targets Nacos 2.0.x versions and is conditionally loaded when
 * Nacos governance is enabled and the target class is present in the classpath.</p>
 */
@Injectable
@Extension(value = "NacosNamingHttpClientProxyDefinition_v2.0", order = PluginDefinition.ORDER_REGISTRY)
@ConditionalOnNacos20Enabled
@ConditionalOnClass(NacosNamingHttpClientProxyDefinition.TYPE)
public class NacosNamingHttpClientProxyDefinition extends PluginDefinitionAdapter {

    protected static final String TYPE = "com.alibaba.nacos.client.naming.remote.http.NamingHttpClientProxy";

    private static final String METHOD = "reqApi";

    private static final String[] ARGUMENTS = new String[]{
            "java.lang.String",
            "java.util.Map",
            "java.util.Map",
            "java.util.List",
            "java.lang.String",
    };

    public NacosNamingHttpClientProxyDefinition() {
        this.matcher = () -> MatcherBuilder.named(TYPE);
        this.interceptors = new InterceptorDefinition[]{
                new InterceptorDefinitionAdapter(
                        MatcherBuilder.named(METHOD).and(MatcherBuilder.arguments(ARGUMENTS)),
                        () -> new NacosNamingHttpClientProxyInterceptor())
        };
    }
}
