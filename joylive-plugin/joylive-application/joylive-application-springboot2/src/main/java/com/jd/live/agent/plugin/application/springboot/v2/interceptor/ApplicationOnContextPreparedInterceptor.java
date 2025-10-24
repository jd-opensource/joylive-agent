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
import com.jd.live.agent.core.bootstrap.AppContext;
import com.jd.live.agent.core.bootstrap.AppListener.AppListenerAdapter;
import com.jd.live.agent.core.bootstrap.AppListenerSupervisor;
import com.jd.live.agent.core.parser.ObjectConverter;
import com.jd.live.agent.core.plugin.definition.InterceptorAdaptor;
import com.jd.live.agent.governance.config.GovernanceConfig;
import com.jd.live.agent.governance.config.McpConfig;
import com.jd.live.agent.plugin.application.springboot.v2.context.SpringAppContext;
import com.jd.live.agent.plugin.application.springboot.v2.mcp.controller.web.WebMcpController;
import com.jd.live.agent.plugin.application.springboot.v2.util.AppLifecycle;
import com.jd.live.agent.plugin.application.springboot.v2.util.SpringUtils;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;

/**
 * Interceptor that handles context prepared events by notifying registered listeners
 */
public class ApplicationOnContextPreparedInterceptor extends InterceptorAdaptor {

    private final GovernanceConfig config;

    private final AppListenerSupervisor supervisor;

    private final ObjectConverter converter;

    public ApplicationOnContextPreparedInterceptor(AppListenerSupervisor supervisor, GovernanceConfig config, ObjectConverter converter) {
        this.supervisor = supervisor;
        this.config = config;
        this.converter = converter;
    }

    @Override
    public void onSuccess(ExecutableContext ctx) {
        AppLifecycle.contextPrepared(() -> {
            McpConfig mcpConfig = config.getMcpConfig();
            if (mcpConfig.isEnabled()) {
                ContextPreparedListener listener = new ContextPreparedListener(converter);
                supervisor.addFirst(listener);
                try {
                    supervisor.onContextPrepared(new SpringAppContext(ctx.getArgument(0)));
                } finally {
                    supervisor.remove(listener);
                }
            } else {
                supervisor.onContextPrepared(new SpringAppContext(ctx.getArgument(0)));
            }
        });
    }

    private static class ContextPreparedListener extends AppListenerAdapter {


        private final ObjectConverter converter;

        ContextPreparedListener(ObjectConverter converter) {
            this.converter = converter;
        }

        @Override
        public void onContextPrepared(AppContext context) {
            if (!(context instanceof SpringAppContext)) {
                return;
            }
            ConfigurableApplicationContext ctx = ((SpringAppContext) context).getContext();
            ConfigurableEnvironment environment = ctx.getEnvironment();
            ConfigurableListableBeanFactory beanFactory = ctx.getBeanFactory();
            if (beanFactory instanceof BeanDefinitionRegistry) {
                if (SpringUtils.isWeb(environment)) {
                    BeanDefinition definition = BeanDefinitionBuilder
                            .genericBeanDefinition(WebMcpController.class)
                            .addPropertyValue("objectConverter", converter)
                            .getBeanDefinition();
                    BeanDefinitionRegistry registry = (BeanDefinitionRegistry) beanFactory;
                    registry.registerBeanDefinition(WebMcpController.NAME, definition);
                }
            }
        }
    }
}
