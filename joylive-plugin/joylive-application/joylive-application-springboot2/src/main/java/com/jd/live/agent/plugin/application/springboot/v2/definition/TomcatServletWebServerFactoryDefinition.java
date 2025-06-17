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
import com.jd.live.agent.core.inject.annotation.Injectable;
import com.jd.live.agent.core.plugin.definition.*;
import com.jd.live.agent.plugin.application.springboot.v2.condition.ConditionalOnTomcatVirtualThreadEnabled;
import com.jd.live.agent.plugin.application.springboot.v2.interceptor.TomcatWebServerFactoryInterceptor;

/**
 * TomcatServletWebServerFactoryDefinition
 */
@Injectable
@Extension(value = "TomcatServletWebServerFactoryDefinition", order = PluginDefinition.ORDER_REGISTRY)
@ConditionalOnTomcatVirtualThreadEnabled
@ConditionalOnClass(TomcatServletWebServerFactoryDefinition.TYPE)
public class TomcatServletWebServerFactoryDefinition extends PluginDefinitionAdapter implements PluginImporter {

    protected static final String TYPE = "org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory";

    private static final String METHOD = "customizeConnector";

    private static final String[] ARGUMENTS = new String[]{
            "org.apache.catalina.connector.Connector"
    };

    public TomcatServletWebServerFactoryDefinition() {
        this.matcher = () -> MatcherBuilder.named(TYPE);
        this.interceptors = new InterceptorDefinition[]{
                new InterceptorDefinitionAdapter(
                        MatcherBuilder.named(METHOD).and(MatcherBuilder.arguments(ARGUMENTS)),
                        () -> new TomcatWebServerFactoryInterceptor()),
        };
    }

    @Override
    public String[] getImports() {
        return new String[]{"java.lang.String"};
    }
}
