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
package com.jd.live.agent.plugin.application.springboot.mcp;

import com.jd.live.agent.core.inject.annotation.Inject;
import com.jd.live.agent.core.inject.annotation.Injectable;
import com.jd.live.agent.core.mcp.handler.McpHandler;
import com.jd.live.agent.core.mcp.version.McpVersion;
import com.jd.live.agent.core.parser.ObjectConverter;
import com.jd.live.agent.core.parser.ObjectParser;
import com.jd.live.agent.governance.invoke.InvocationContext;
import com.jd.live.agent.plugin.application.springboot.mcp.reactive.ReactiveMcpController;
import com.jd.live.agent.plugin.application.springboot.mcp.web.jakarta.JakartaWebMcpController;
import com.jd.live.agent.plugin.application.springboot.mcp.web.javax.JavaxWebMcpController;
import com.jd.live.agent.plugin.application.springboot.register.BeanRegister;
import com.jd.live.agent.plugin.application.springboot.util.SpringUtils;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;

import java.util.Map;

@Injectable
public class McpBeanRegister implements BeanRegister {

    @Inject(InvocationContext.COMPONENT_INVOCATION_CONTEXT)
    private InvocationContext context;

    @Inject
    private Map<String, McpHandler> handlers;

    @Inject
    private ObjectConverter converter;

    @Inject(ObjectParser.JSON)
    private ObjectParser parser;

    @Inject
    private Map<String, McpVersion> versions;

    @Inject
    private McpVersion defaultVersion;

    @Override
    public void register(ConfigurableApplicationContext ctx) {
        if (!context.getGovernanceConfig().getMcpConfig().isEnabled()) {
            return;
        }
        ConfigurableEnvironment environment = ctx.getEnvironment();
        ConfigurableListableBeanFactory beanFactory = ctx.getBeanFactory();
        if (beanFactory instanceof BeanDefinitionRegistry) {
            Class<?> mcpType = null;
            String beanName = null;
            if (SpringUtils.isWeb(environment)) {
                if (SpringUtils.isJavaxServlet()) {
                    mcpType = JavaxWebMcpController.class;
                    beanName = JavaxWebMcpController.NAME;
                } else {
                    mcpType = JakartaWebMcpController.class;
                    beanName = JakartaWebMcpController.NAME;
                }
            } else if (SpringUtils.isWebFlux(environment)) {
                mcpType = ReactiveMcpController.class;
                beanName = ReactiveMcpController.NAME;
            }
            if (mcpType != null) {
                BeanDefinition definition = BeanDefinitionBuilder
                        .genericBeanDefinition(mcpType)
                        .addPropertyValue("context", context)
                        .addPropertyValue("objectConverter", converter)
                        .addPropertyValue("objectParser", parser)
                        .addPropertyValue("handlers", handlers)
                        .addPropertyValue("config", context.getGovernanceConfig())
                        .addPropertyValue("versions", versions)
                        .addPropertyValue("defaultVersion", defaultVersion)
                        .getBeanDefinition();
                BeanDefinitionRegistry registry = (BeanDefinitionRegistry) beanFactory;
                registry.registerBeanDefinition(beanName, definition);
            }
        }
    }

}
