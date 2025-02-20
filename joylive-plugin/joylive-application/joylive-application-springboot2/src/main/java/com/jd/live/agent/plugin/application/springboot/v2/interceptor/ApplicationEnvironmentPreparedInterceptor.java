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
import com.jd.live.agent.core.plugin.definition.InterceptorAdaptor;
import com.jd.live.agent.plugin.application.springboot.v2.context.SpringAppBootstrapContext;
import com.jd.live.agent.plugin.application.springboot.v2.context.SpringAppEnvironment;
import com.jd.live.agent.plugin.application.springboot.v2.listener.InnerListener;

/**
 * An interceptor that adds a Configurator-based PropertySource to the ConfigurableEnvironment
 * during the onEnter phase of the executable context.
 *
 * @since 1.6.0
 */
public class ApplicationEnvironmentPreparedInterceptor extends InterceptorAdaptor {

    private final AppListener listener;

    public ApplicationEnvironmentPreparedInterceptor(AppListener listener) {
        this.listener = listener;
    }

    @Override
    public void onEnter(ExecutableContext ctx) {
        SpringAppBootstrapContext context = new SpringAppBootstrapContext();
        SpringAppEnvironment environment = new SpringAppEnvironment(ctx.getArgument(1));
        InnerListener.foreach(l -> l.onEnvironmentPrepared(context, environment));
        listener.onEnvironmentPrepared(context, environment);
    }
}
