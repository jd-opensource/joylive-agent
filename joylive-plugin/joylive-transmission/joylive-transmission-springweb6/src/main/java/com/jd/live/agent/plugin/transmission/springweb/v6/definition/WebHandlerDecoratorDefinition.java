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
package com.jd.live.agent.plugin.transmission.springweb.v6.definition;

import com.jd.live.agent.core.bytekit.matcher.MatcherBuilder;
import com.jd.live.agent.core.extension.annotation.ConditionalOnClass;
import com.jd.live.agent.core.extension.annotation.Extension;
import com.jd.live.agent.core.inject.annotation.Inject;
import com.jd.live.agent.core.inject.annotation.Injectable;
import com.jd.live.agent.core.plugin.definition.InterceptorDefinition;
import com.jd.live.agent.core.plugin.definition.InterceptorDefinitionAdapter;
import com.jd.live.agent.core.plugin.definition.PluginDefinition;
import com.jd.live.agent.core.plugin.definition.PluginDefinitionAdapter;
import com.jd.live.agent.governance.context.bag.Propagation;
import com.jd.live.agent.plugin.transmission.springweb.v6.condition.ConditionalOnSpringWeb6TransmissionEnabled;
import com.jd.live.agent.plugin.transmission.springweb.v6.interceptor.WebHandlerDecoratorInterceptor;

/**
 * WebHandlerDecoratorDefinition
 *
 * @since 1.9.0
 */
@Injectable
@Extension(value = "WebHandlerDecoratorDefinition_v6", order = PluginDefinition.ORDER_TRANSMISSION)
@ConditionalOnSpringWeb6TransmissionEnabled
@ConditionalOnClass(WebHandlerDecoratorDefinition.TYPE_FILTERING_WEB_HANDLER)
@ConditionalOnClass(WebHandlerDecoratorDefinition.TYPE_MONO)
public class WebHandlerDecoratorDefinition extends PluginDefinitionAdapter {

    protected static final String TYPE_FILTERING_WEB_HANDLER = "org.springframework.web.server.handler.WebHandlerDecorator";

    protected static final String TYPE_MONO = "reactor.core.publisher.Mono";

    private static final String METHOD_HANDLE = "handle";

    private static final String[] ARGUMENT_HANDLE = new String[]{
            "org.springframework.web.server.ServerWebExchange"
    };

    @Inject(value = Propagation.COMPONENT_PROPAGATION, component = true)
    private Propagation propagation;

    public WebHandlerDecoratorDefinition() {
        this.matcher = () -> MatcherBuilder.named(TYPE_FILTERING_WEB_HANDLER);
        this.interceptors = new InterceptorDefinition[]{
                new InterceptorDefinitionAdapter(
                        MatcherBuilder.named(METHOD_HANDLE).
                                and(MatcherBuilder.arguments(ARGUMENT_HANDLE)),
                        () -> new WebHandlerDecoratorInterceptor(propagation))};
    }
}
