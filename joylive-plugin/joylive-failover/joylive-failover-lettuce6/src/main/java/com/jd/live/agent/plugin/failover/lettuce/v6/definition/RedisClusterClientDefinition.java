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
package com.jd.live.agent.plugin.failover.lettuce.v6.definition;

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
import com.jd.live.agent.plugin.failover.lettuce.v6.condition.ConditionalOnFailoverLettuce6Enabled;
import com.jd.live.agent.plugin.failover.lettuce.v6.interceptor.ConnectClusterAsyncInterceptor;
import com.jd.live.agent.plugin.failover.lettuce.v6.interceptor.ConnectClusterPubSubAsyncInterceptor;

@Injectable
@Extension(value = "RedisClusterClientDefinition_v6", order = PluginDefinition.ORDER_FAILOVER)
@ConditionalOnFailoverLettuce6Enabled
@ConditionalOnClass(RedisClusterClientDefinition.TYPE)
public class RedisClusterClientDefinition extends PluginDefinitionAdapter {

    protected static final String TYPE = "io.lettuce.core.cluster.RedisClusterClient";

    private static final String METHOD_CONNECT_CLUSTER_ASYNC = "connectClusterAsync";

    private static final String[] ARGUMENTS_CONNECT_CLUSTER_ASYNC = new String[]{
            "io.lettuce.core.codec.RedisCodec",
    };

    private static final String METHOD_CONNECT_CLUSTER_PUBSUB_ASYNC = "connectClusterPubSubAsync";

    private static final String[] ARGUMENTS_CONNECT_CLUSTER_PUBSUB_ASYNC = new String[]{
            "io.lettuce.core.codec.RedisCodec"
    };

    @Inject(InvocationContext.COMPONENT_INVOCATION_CONTEXT)
    private InvocationContext context;

    public RedisClusterClientDefinition() {
        this.matcher = () -> MatcherBuilder.named(TYPE);
        this.interceptors = new InterceptorDefinition[]{
                new InterceptorDefinitionAdapter(MatcherBuilder.named(METHOD_CONNECT_CLUSTER_ASYNC)
                        .and(MatcherBuilder.arguments(ARGUMENTS_CONNECT_CLUSTER_ASYNC)),
                        () -> new ConnectClusterAsyncInterceptor(context)),
                new InterceptorDefinitionAdapter(MatcherBuilder.named(METHOD_CONNECT_CLUSTER_PUBSUB_ASYNC)
                        .and(MatcherBuilder.arguments(ARGUMENTS_CONNECT_CLUSTER_PUBSUB_ASYNC)),
                        () -> new ConnectClusterPubSubAsyncInterceptor(context)),
        };
    }
}
