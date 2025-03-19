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
package com.jd.live.agent.plugin.application.springboot.v2.interceptor;

import com.jd.live.agent.bootstrap.bytekit.context.ExecutableContext;
import com.jd.live.agent.core.bootstrap.AppListener;
import com.jd.live.agent.core.instance.Application;
import com.jd.live.agent.core.plugin.definition.InterceptorAdaptor;
import com.jd.live.agent.governance.config.GovernanceConfig;
import com.jd.live.agent.governance.registry.Registry;
import com.jd.live.agent.plugin.application.springboot.v2.context.SpringAppBootstrapContext;
import com.jd.live.agent.plugin.application.springboot.v2.context.SpringAppEnvironment;
import com.jd.live.agent.plugin.application.springboot.v2.listener.InnerListener;
import com.jd.live.agent.plugin.application.springboot.v2.util.AppLifecycle;
import org.springframework.core.env.ConfigurableEnvironment;

/**
 * An interceptor that adds a Configurator-based PropertySource to the ConfigurableEnvironment
 * during the onEnter phase of the executable context.
 *
 * @since 1.6.0
 */
public class ApplicationEnvironmentPreparedInterceptor extends InterceptorAdaptor {

    private final AppListener listener;

    private final GovernanceConfig config;

    private final Registry registry;

    private final Application application;

    public ApplicationEnvironmentPreparedInterceptor(AppListener listener, GovernanceConfig config, Registry registry, Application application) {
        this.listener = listener;
        this.config = config;
        this.registry = registry;
        this.application = application;
    }

    @Override
    public void onSuccess(ExecutableContext ctx) {
        SpringAppBootstrapContext context = new SpringAppBootstrapContext();
        Object[] arguments = ctx.getArguments();
        ConfigurableEnvironment env = (ConfigurableEnvironment) (arguments.length > 1 ? arguments[1] : arguments[0]);
        SpringAppEnvironment environment = new SpringAppEnvironment(env);
        // fix for spring boot 2.1, it will trigger twice.
        AppLifecycle.prepared(() -> {
            if (config.getRegistryConfig().isEnabled()) {
                // subscribe policy
                registry.register(application.getService().getName(), application.getService().getGroup());
            }
            InnerListener.foreach(l -> l.onEnvironmentPrepared(context, environment));
            listener.onEnvironmentPrepared(context, environment);
        });
    }
}
