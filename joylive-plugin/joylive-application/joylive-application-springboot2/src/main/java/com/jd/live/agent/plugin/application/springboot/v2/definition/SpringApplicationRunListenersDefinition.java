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

import com.jd.live.agent.core.bootstrap.AppListener;
import com.jd.live.agent.core.bytekit.matcher.MatcherBuilder;
import com.jd.live.agent.core.extension.annotation.ConditionalOnClass;
import com.jd.live.agent.core.extension.annotation.Extension;
import com.jd.live.agent.core.inject.annotation.Inject;
import com.jd.live.agent.core.inject.annotation.Injectable;
import com.jd.live.agent.core.instance.Application;
import com.jd.live.agent.core.plugin.definition.InterceptorDefinition;
import com.jd.live.agent.core.plugin.definition.InterceptorDefinitionAdapter;
import com.jd.live.agent.core.plugin.definition.PluginDefinition;
import com.jd.live.agent.core.plugin.definition.PluginDefinitionAdapter;
import com.jd.live.agent.governance.annotation.ConditionalOnGovernanceEnabled;
import com.jd.live.agent.governance.config.GovernanceConfig;
import com.jd.live.agent.governance.doc.DocumentRegistry;
import com.jd.live.agent.governance.registry.Registry;
import com.jd.live.agent.plugin.application.springboot.v2.interceptor.ApplicationOnEnvironmentPreparedInterceptor;
import com.jd.live.agent.plugin.application.springboot.v2.interceptor.ApplicationOnReadyInterceptor;
import com.jd.live.agent.plugin.application.springboot.v2.interceptor.ApplicationOnStartedInterceptor;

@Injectable
@Extension(value = "SpringApplicationRunListenersDefinition_v5", order = PluginDefinition.ORDER_APPLICATION)
@ConditionalOnGovernanceEnabled
@ConditionalOnClass(SpringApplicationRunListenersDefinition.TYPE_SPRING_APPLICATION_RUN_LISTENERS)
public class SpringApplicationRunListenersDefinition extends PluginDefinitionAdapter {

    protected static final String TYPE_SPRING_APPLICATION_RUN_LISTENERS = "org.springframework.boot.SpringApplicationRunListeners";

    private static final String METHOD_STARTED = "started";

    // for springboot 2.2.9+
    private static final String METHOD_READY = "ready";

    // fix for springboot 2.2.9-
    private static final String METHOD_RUNNING = "running";

    // fix for spring boot 1.x
    private static final String METHOD_FINISHED = "finished";

    private static final String METHOD_ENVIRONMENT_PREPARED = "environmentPrepared";

    @Inject(value = AppListener.COMPONENT_APPLICATION_LISTENER, component = true)
    private AppListener listener;

    @Inject(GovernanceConfig.COMPONENT_GOVERNANCE_CONFIG)
    private GovernanceConfig config;

    @Inject(Registry.COMPONENT_REGISTRY)
    private Registry registry;

    @Inject(DocumentRegistry.COMPONENT_SERVICE_DOC_REGISTRY)
    private DocumentRegistry docRegistry;

    @Inject(Application.COMPONENT_APPLICATION)
    private Application application;

    public SpringApplicationRunListenersDefinition() {
        this.matcher = () -> MatcherBuilder.named(TYPE_SPRING_APPLICATION_RUN_LISTENERS);
        this.interceptors = new InterceptorDefinition[]{
                new InterceptorDefinitionAdapter(MatcherBuilder.named(METHOD_STARTED),
                        () -> new ApplicationOnStartedInterceptor(listener, docRegistry, application)),
                new InterceptorDefinitionAdapter(MatcherBuilder.in(METHOD_READY, METHOD_RUNNING, METHOD_FINISHED),
                        () -> new ApplicationOnReadyInterceptor(listener, config, registry, application)),
                new InterceptorDefinitionAdapter(MatcherBuilder.in(METHOD_ENVIRONMENT_PREPARED),
                        () -> new ApplicationOnEnvironmentPreparedInterceptor(listener, config, registry, application))
        };
    }
}
