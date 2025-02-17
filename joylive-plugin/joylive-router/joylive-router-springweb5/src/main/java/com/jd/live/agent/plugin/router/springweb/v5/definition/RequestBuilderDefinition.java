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
package com.jd.live.agent.plugin.router.springweb.v5.definition;

import com.jd.live.agent.core.bytekit.matcher.MatcherBuilder;
import com.jd.live.agent.core.extension.annotation.ConditionalOnClass;
import com.jd.live.agent.core.extension.annotation.Extension;
import com.jd.live.agent.core.plugin.definition.InterceptorDefinition;
import com.jd.live.agent.core.plugin.definition.InterceptorDefinitionAdapter;
import com.jd.live.agent.core.plugin.definition.PluginDefinitionAdapter;
import com.jd.live.agent.plugin.router.springweb.v5.condition.ConditionalOnSpringWeb5GovernanceEnabled;
import com.jd.live.agent.plugin.router.springweb.v5.interceptor.RequestBuilderInterceptor;

/**
 * RequestBuilderDefinition
 */
@Extension(value = "RequestBuilderDefinition_v5")
@ConditionalOnSpringWeb5GovernanceEnabled
@ConditionalOnClass(RequestBuilderDefinition.TYPE)
@ConditionalOnClass(RequestBuilderDefinition.REACTOR_MONO)
@Deprecated
public class RequestBuilderDefinition extends PluginDefinitionAdapter {

    protected static final String TYPE = "org.springframework.http.server.reactive.DefaultServerHttpRequestBuilder";

    protected static final String REACTOR_MONO = "reactor.core.publisher.Mono";

    private static final String METHOD = "getUriToUse";

    public RequestBuilderDefinition() {
        this.matcher = () -> MatcherBuilder.named(TYPE);
        this.interceptors = new InterceptorDefinition[]{
                new InterceptorDefinitionAdapter(MatcherBuilder.named(METHOD), RequestBuilderInterceptor::new)
        };
    }
}
