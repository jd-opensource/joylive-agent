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
package com.jd.live.agent.plugin.router.springcloud.v1.definition;

import com.jd.live.agent.core.bytekit.matcher.MatcherBuilder;
import com.jd.live.agent.core.extension.annotation.ConditionalOnClass;
import com.jd.live.agent.core.extension.annotation.Extension;
import com.jd.live.agent.core.inject.annotation.Inject;
import com.jd.live.agent.core.inject.annotation.Injectable;
import com.jd.live.agent.core.plugin.definition.InterceptorDefinition;
import com.jd.live.agent.core.plugin.definition.InterceptorDefinitionAdapter;
import com.jd.live.agent.core.plugin.definition.PluginDefinitionAdapter;
import com.jd.live.agent.governance.invoke.InvocationContext;
import com.jd.live.agent.plugin.router.springcloud.v1.condition.ConditionalOnSpringCloud1FlowControlEnabled;
import com.jd.live.agent.plugin.router.springcloud.v1.interceptor.HttpClientCloudClusterInterceptor;

/**
 * HttpClientCloudClusterDefinition
 *
 * @since 1.9.0
 */
@Injectable
@Extension(value = "HttpClientCloudClusterDefinition_v1")
@ConditionalOnSpringCloud1FlowControlEnabled
@ConditionalOnClass(HttpClientCloudClusterDefinition.TYPE_RIBBON_LOAD_BALANCING_HTTP_CLIENT)
public class HttpClientCloudClusterDefinition extends PluginDefinitionAdapter {

    protected static final String TYPE_RIBBON_LOAD_BALANCING_HTTP_CLIENT = "org.springframework.cloud.netflix.ribbon.apache.RibbonLoadBalancingHttpClient";

    protected static final String TYPE_RETRYABLE_RIBBON_LOAD_BALANCING_HTTP_CLIENT = "org.springframework.cloud.netflix.ribbon.apache.RetryableRibbonLoadBalancingHttpClient";

    @Inject(InvocationContext.COMPONENT_INVOCATION_CONTEXT)
    private InvocationContext context;

    public HttpClientCloudClusterDefinition() {
        this.matcher = () -> MatcherBuilder.in(TYPE_RETRYABLE_RIBBON_LOAD_BALANCING_HTTP_CLIENT, TYPE_RIBBON_LOAD_BALANCING_HTTP_CLIENT);
        this.interceptors = new InterceptorDefinition[]{
                new InterceptorDefinitionAdapter(MatcherBuilder.isConstructor(),
                        () -> new HttpClientCloudClusterInterceptor(context))
        };
    }
}
