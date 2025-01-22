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
package com.jd.live.agent.plugin.router.gprc.definition;

import com.jd.live.agent.core.bytekit.matcher.MatcherBuilder;
import com.jd.live.agent.core.extension.annotation.ConditionalOnClass;
import com.jd.live.agent.core.extension.annotation.Extension;
import com.jd.live.agent.core.inject.annotation.Injectable;
import com.jd.live.agent.core.plugin.definition.InterceptorDefinition;
import com.jd.live.agent.core.plugin.definition.InterceptorDefinitionAdapter;
import com.jd.live.agent.core.plugin.definition.PluginDefinition;
import com.jd.live.agent.core.plugin.definition.PluginDefinitionAdapter;
import com.jd.live.agent.governance.annotation.ConditionalOnFlowControlEnabled;
import com.jd.live.agent.plugin.router.gprc.interceptor.ServerObserverInterceptor;

@Injectable
@Extension(value = "ServerObserverDefinition", order = PluginDefinition.ORDER_ROUTER)
@ConditionalOnFlowControlEnabled
@ConditionalOnClass(ServerObserverDefinition.TYPE_SERVER_CALL_STREAM_OBSERVER_IMPL)
public class ServerObserverDefinition extends PluginDefinitionAdapter {

    public static final String TYPE_SERVER_CALL_STREAM_OBSERVER_IMPL = "io.grpc.stub.ServerCalls$ServerCallStreamObserverImpl";

    private static final String METHOD_ON_ERROR = "onError";

    public ServerObserverDefinition() {
        this.matcher = () -> MatcherBuilder.named(TYPE_SERVER_CALL_STREAM_OBSERVER_IMPL);
        this.interceptors = new InterceptorDefinition[]{
                new InterceptorDefinitionAdapter(
                        MatcherBuilder.named(METHOD_ON_ERROR),
                        ServerObserverInterceptor::new)
        };

    }

}
