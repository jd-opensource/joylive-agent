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
package com.jd.live.agent.plugin.application.springboot.v2.definition;

import com.jd.live.agent.core.bytekit.matcher.MatcherBuilder;
import com.jd.live.agent.core.extension.annotation.ConditionalOnClass;
import com.jd.live.agent.core.extension.annotation.Extension;
import com.jd.live.agent.core.inject.annotation.Inject;
import com.jd.live.agent.core.inject.annotation.Injectable;
import com.jd.live.agent.core.plugin.definition.InterceptorDefinition;
import com.jd.live.agent.core.plugin.definition.InterceptorDefinitionAdapter;
import com.jd.live.agent.core.plugin.definition.PluginDefinition;
import com.jd.live.agent.core.plugin.definition.PluginDefinitionAdapter;
import com.jd.live.agent.core.security.StringDecrypter;
import com.jd.live.agent.governance.annotation.ConditionalOnCipherEnabled;
import com.jd.live.agent.plugin.application.springboot.v2.interceptor.AbstractPropertyResolverInterceptor;

@Injectable
@Extension(value = "AbstractPropertyResolverDefinition_v5", order = PluginDefinition.ORDER_APPLICATION)
@ConditionalOnCipherEnabled
@ConditionalOnClass(AbstractPropertyResolverDefinition.TYPE)
public class AbstractPropertyResolverDefinition extends PluginDefinitionAdapter {

    protected static final String TYPE = "org.springframework.core.env.AbstractPropertyResolver";

    // this method is only called by ConfigurationPropertySourcesPropertyResolver and PropertySourcesPropertyResolver
    private static final String METHOD = "convertValueIfNecessary";

    private static final String[] ARGUMENTS = new String[]{
            "java.lang.Object",
            "java.lang.Class"
    };

    @Inject(StringDecrypter.COMPONENT_STRING_DECRYPTER)
    private StringDecrypter decrypter;

    public AbstractPropertyResolverDefinition() {
        this.matcher = () -> MatcherBuilder.named(TYPE);
        this.interceptors = new InterceptorDefinition[]{
                new InterceptorDefinitionAdapter(MatcherBuilder.named(METHOD).and(MatcherBuilder.arguments(ARGUMENTS)),
                        () -> new AbstractPropertyResolverInterceptor(decrypter))
        };
    }
}
