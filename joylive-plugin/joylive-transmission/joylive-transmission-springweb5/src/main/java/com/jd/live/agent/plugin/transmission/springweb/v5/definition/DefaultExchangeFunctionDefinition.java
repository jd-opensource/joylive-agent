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
package com.jd.live.agent.plugin.transmission.springweb.v5.definition;

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
import com.jd.live.agent.plugin.transmission.springweb.v5.condition.ConditionalOnSpringWeb5TransmissionEnabled;
import com.jd.live.agent.plugin.transmission.springweb.v5.interceptor.DefaultExchangeFunctionInterceptor;

/**
 * DefaultExchangeFunctionDefinition
 *
 * @since 1.0.0
 */
@Injectable
@Extension(value = "DefaultExchangeFunctionDefinition_v5", order = PluginDefinition.ORDER_TRANSMISSION)
@ConditionalOnSpringWeb5TransmissionEnabled
@ConditionalOnClass(DefaultExchangeFunctionDefinition.TYPE_DEFAULT_EXCHANGE_FUNCTION)
public class DefaultExchangeFunctionDefinition extends PluginDefinitionAdapter {

    protected static final String TYPE_DEFAULT_EXCHANGE_FUNCTION = "org.springframework.web.reactive.function.client.ExchangeFunction";

    private static final String METHOD_EXCHANGE = "exchange";

    private static final String[] ARGUMENT_EXCHANGE = new String[]{
            "org.springframework.web.reactive.function.client.ClientRequest"
    };

    @Inject(value = Propagation.COMPONENT_PROPAGATION, component = true)
    private Propagation propagation;

    public DefaultExchangeFunctionDefinition() {
        this.matcher = () -> MatcherBuilder.isImplement(TYPE_DEFAULT_EXCHANGE_FUNCTION);
        this.interceptors = new InterceptorDefinition[]{
                new InterceptorDefinitionAdapter(
                        MatcherBuilder.named(METHOD_EXCHANGE).
                                and(MatcherBuilder.arguments(ARGUMENT_EXCHANGE)),
                        () -> new DefaultExchangeFunctionInterceptor(propagation))
        };
    }
}
