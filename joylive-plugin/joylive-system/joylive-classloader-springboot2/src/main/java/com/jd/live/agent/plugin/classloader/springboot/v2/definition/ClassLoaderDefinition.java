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
package com.jd.live.agent.plugin.classloader.springboot.v2.definition;

import com.jd.live.agent.bootstrap.classloader.Resourcer;
import com.jd.live.agent.core.bytekit.matcher.MatcherBuilder;
import com.jd.live.agent.core.config.ClassLoaderConfig;
import com.jd.live.agent.core.extension.annotation.ConditionalOnClass;
import com.jd.live.agent.core.extension.annotation.ConditionalOnProperty;
import com.jd.live.agent.core.extension.annotation.Extension;
import com.jd.live.agent.core.inject.annotation.Inject;
import com.jd.live.agent.core.inject.annotation.Injectable;
import com.jd.live.agent.core.plugin.definition.InterceptorDefinition;
import com.jd.live.agent.core.plugin.definition.InterceptorDefinitionAdapter;
import com.jd.live.agent.core.plugin.definition.PluginDefinition;
import com.jd.live.agent.core.plugin.definition.PluginDefinitionAdapter;
import com.jd.live.agent.governance.config.GovernanceConfig;
import com.jd.live.agent.plugin.classloader.springboot.v2.interceptor.ClassLoaderFindResourceInterceptor;
import com.jd.live.agent.plugin.classloader.springboot.v2.interceptor.ClassLoaderLoadClassInterceptor;

@Injectable
@Extension(value = "ClassLoaderDefinition", order = PluginDefinition.ORDER_SYSTEM)
@ConditionalOnProperty(GovernanceConfig.CONFIG_CLASSLOADER_SPRING_BOOT_ENABLED)
@ConditionalOnProperty(GovernanceConfig.CONFIG_CLASSLOADER_ENABLED)
@ConditionalOnClass(ClassLoaderDefinition.TYPE_LAUNCHED_URL_CLASSLOADER)
public class ClassLoaderDefinition extends PluginDefinitionAdapter {

    protected static final String TYPE_LAUNCHED_URL_CLASSLOADER = "org.springframework.boot.loader.LaunchedURLClassLoader";

    private static final String METHOD_LOAD_CLASS = "loadClass";

    private static final String METHOD_FIND_RESOURCE = "findResource";

    private static final String[] ARGUMENT_LOAD_CLASS = new String[]{
            "java.lang.String",
            "boolean"
    };

    private static final String[] ARGUMENT_FIND_RESOURCE = new String[]{
            "java.lang.String"
    };

    @Inject(Resourcer.COMPONENT_RESOURCER)
    private Resourcer resourcer;

    @Inject(ClassLoaderConfig.COMPONENT_CLASSLOADER_CONFIG)
    private ClassLoaderConfig classLoaderConfig;

    public ClassLoaderDefinition() {
        this.matcher = () -> MatcherBuilder.named(TYPE_LAUNCHED_URL_CLASSLOADER);
        this.interceptors = new InterceptorDefinition[]{
                new InterceptorDefinitionAdapter(
                        MatcherBuilder.named(METHOD_LOAD_CLASS).
                                and(MatcherBuilder.arguments(ARGUMENT_LOAD_CLASS)),
                        () -> new ClassLoaderLoadClassInterceptor(resourcer, classLoaderConfig)),
                new InterceptorDefinitionAdapter(
                        MatcherBuilder.named(METHOD_FIND_RESOURCE).
                                and(MatcherBuilder.arguments(ARGUMENT_FIND_RESOURCE)),
                        () -> new ClassLoaderFindResourceInterceptor(resourcer, classLoaderConfig))
        };
    }
}
