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
package com.jd.live.agent.plugin.router.sofarpc.definition;

import com.jd.live.agent.core.bytekit.matcher.MatcherBuilder;
import com.jd.live.agent.core.extension.annotation.ConditionalOnClass;
import com.jd.live.agent.core.extension.annotation.Extension;
import com.jd.live.agent.core.plugin.definition.InterceptorDefinition;
import com.jd.live.agent.core.plugin.definition.InterceptorDefinitionAdapter;
import com.jd.live.agent.core.plugin.definition.PluginDefinitionAdapter;
import com.jd.live.agent.plugin.router.sofarpc.condition.ConditionalOnSofaRpcFlowControlEnabled;
import com.jd.live.agent.plugin.router.sofarpc.interceptor.ResponseFutureConstructorInterceptor;
import com.jd.live.agent.plugin.router.sofarpc.interceptor.ResponseFutureNotifyListenersInterceptor;

@Extension(value = "ResponseFutureDefinition")
@ConditionalOnSofaRpcFlowControlEnabled
@ConditionalOnClass(ResponseFutureDefinition.TYPE)
public class ResponseFutureDefinition extends PluginDefinitionAdapter {

    protected static final String TYPE = "com.alipay.sofa.rpc.message.AbstractResponseFuture";

    private static final String[] ARGUMENTS_CONSTRUCTOR = new String[]{
            "com.alipay.sofa.rpc.core.request.SofaRequest",
            "int"
    };

    private static final String METHOD = "notifyListeners";

    public ResponseFutureDefinition() {
        this.matcher = () -> MatcherBuilder.isSubTypeOf(TYPE)
                .and(MatcherBuilder.not(MatcherBuilder.isAbstract()));
        this.interceptors = new InterceptorDefinition[]{
                new InterceptorDefinitionAdapter(
                        MatcherBuilder.isConstructor().and(MatcherBuilder.arguments(ARGUMENTS_CONSTRUCTOR)),
                        () -> new ResponseFutureConstructorInterceptor()
                ),
                new InterceptorDefinitionAdapter(MatcherBuilder.named(METHOD), () -> new ResponseFutureNotifyListenersInterceptor())
        };
    }
}
