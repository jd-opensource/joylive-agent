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
package com.jd.live.agent.plugin.router.kafka.v2.definition;

import com.jd.live.agent.core.bytekit.matcher.MatcherBuilder;
import com.jd.live.agent.core.extension.annotation.ConditionalOnClass;
import com.jd.live.agent.core.extension.annotation.Extension;
import com.jd.live.agent.core.inject.annotation.Inject;
import com.jd.live.agent.core.inject.annotation.Injectable;
import com.jd.live.agent.core.plugin.definition.InterceptorDefinition;
import com.jd.live.agent.core.plugin.definition.InterceptorDefinitionAdapter;
import com.jd.live.agent.core.plugin.definition.PluginDefinitionAdapter;
import com.jd.live.agent.governance.invoke.InvocationContext;
import com.jd.live.agent.plugin.router.kafka.v2.condition.ConditionalOnKafka2AnyRouteEnabled;
import com.jd.live.agent.plugin.router.kafka.v2.interceptor.FetchRecordsInterceptor;

/**
 * CompletedFetchDefinition
 *
 * @since 1.8.0
 */
@Injectable
@Extension(value = "CompletedFetchDefinition_v2")
@ConditionalOnKafka2AnyRouteEnabled
@ConditionalOnClass(CompletedFetchDefinition.TYPE)
public class CompletedFetchDefinition extends PluginDefinitionAdapter {

    protected static final String TYPE = "org.apache.kafka.clients.consumer.internals.Fetcher$CompletedFetch";

    private static final String METHOD = "fetchRecords";

    @Inject(InvocationContext.COMPONENT_INVOCATION_CONTEXT)
    private InvocationContext context;

    public CompletedFetchDefinition() {
        this.matcher = () -> MatcherBuilder.named(TYPE);
        this.interceptors = new InterceptorDefinition[]{
                new InterceptorDefinitionAdapter(
                        MatcherBuilder.named(METHOD).and(MatcherBuilder.arguments(2)),
                        () -> new FetchRecordsInterceptor(context)
                )
        };
    }
}
