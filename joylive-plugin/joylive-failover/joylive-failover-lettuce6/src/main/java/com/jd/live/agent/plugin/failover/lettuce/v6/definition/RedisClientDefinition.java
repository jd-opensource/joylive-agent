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
import com.jd.live.agent.plugin.failover.lettuce.v6.interceptor.ConnectPubSubAsyncInterceptor;
import com.jd.live.agent.plugin.failover.lettuce.v6.interceptor.ConnectSentinelAsyncInterceptor;
import com.jd.live.agent.plugin.failover.lettuce.v6.interceptor.ConnectStandaloneAsyncInterceptor;
import com.jd.live.agent.plugin.failover.lettuce.v6.interceptor.IgnoreSentinelInterceptor;

@Injectable
@Extension(value = "RedisClientDefinition_v6", order = PluginDefinition.ORDER_FAILOVER)
@ConditionalOnFailoverLettuce6Enabled
@ConditionalOnClass(RedisClientDefinition.TYPE)
public class RedisClientDefinition extends PluginDefinitionAdapter {

    protected static final String TYPE = "io.lettuce.core.RedisClient";

    private static final String METHOD_CONNECT_STANDALONE_ASYNC = "connectStandaloneAsync";

    private static final String[] ARGUMENTS_CONNECT_STANDALONE_ASYNC = new String[]{
            "io.lettuce.core.codec.RedisCodec",
            "io.lettuce.core.RedisURI",
            "java.time.Duration"
    };

    private static final String METHOD_CONNECT_SENTINEL_ASYNC = "connectSentinelAsync";

    private static final String[] ARGUMENTS_CONNECT_SENTINEL_ASYNC = new String[]{
            "io.lettuce.core.codec.RedisCodec",
            "io.lettuce.core.RedisURI",
            "java.time.Duration"
    };

    private static final String METHOD_CONNECT_PUBSUB_ASYNC = "connectPubSubAsync";

    private static final String[] ARGUMENTS_CONNECT_PUBSUB_ASYNC = new String[]{
            "io.lettuce.core.codec.RedisCodec",
            "io.lettuce.core.RedisURI",
            "java.time.Duration"
    };

    private static final String METHOD_LOOKUP_REDIS = "lookupRedis";

    private static final String[] ARGUMENTS_LOOKUP_REDIS = new String[]{
            "io.lettuce.core.RedisURI",
    };

    @Inject(InvocationContext.COMPONENT_INVOCATION_CONTEXT)
    private InvocationContext context;

    public RedisClientDefinition() {
        this.matcher = () -> MatcherBuilder.named(TYPE);
        this.interceptors = new InterceptorDefinition[]{
                new InterceptorDefinitionAdapter(MatcherBuilder.named(METHOD_CONNECT_STANDALONE_ASYNC)
                        .and(MatcherBuilder.arguments(ARGUMENTS_CONNECT_STANDALONE_ASYNC)),
                        () -> new ConnectStandaloneAsyncInterceptor(context)),
                new InterceptorDefinitionAdapter(MatcherBuilder.named(METHOD_CONNECT_PUBSUB_ASYNC)
                        .and(MatcherBuilder.arguments(ARGUMENTS_CONNECT_PUBSUB_ASYNC)),
                        () -> new ConnectPubSubAsyncInterceptor(context)),
                new InterceptorDefinitionAdapter(MatcherBuilder.named(METHOD_CONNECT_SENTINEL_ASYNC)
                        .and(MatcherBuilder.arguments(ARGUMENTS_CONNECT_SENTINEL_ASYNC)),
                        () -> new ConnectSentinelAsyncInterceptor(context)),
                new InterceptorDefinitionAdapter(MatcherBuilder.named(METHOD_LOOKUP_REDIS)
                        .and(MatcherBuilder.arguments(ARGUMENTS_LOOKUP_REDIS)),
                        () -> new IgnoreSentinelInterceptor()),
        };
    }
}
