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
package com.jd.live.agent.plugin.application.springboot.context;

import com.jd.live.agent.core.bootstrap.AppBeanDefinition;
import com.jd.live.agent.core.bootstrap.WebType;
import com.jd.live.agent.core.util.cache.LazyObject;
import com.jd.live.agent.governance.bootstrap.ConfigurableAppContext;
import com.jd.live.agent.governance.subscription.config.ConfigCenter;
import com.jd.live.agent.plugin.application.springboot.config.SpringConfigRefresher;
import com.jd.live.agent.plugin.application.springboot.util.SpringUtils;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;

/**
 * An implementation of the ApplicationContext interface for Spring-based applications.
 */
public class SpringAppContext implements ConfigurableAppContext {

    private final ConfigurableApplicationContext context;

    /**
     * The refresher responsible for refreshing the environment.
     */
    private final SpringConfigRefresher refresher;

    private final LazyObject<WebType> webType;

    /**
     * Constructs a new SpringApplicationContext instance.
     *
     * @param context The Spring application context.
     */
    public SpringAppContext(ConfigurableApplicationContext context) {
        this.context = context;
        this.refresher = new SpringConfigRefresher(context);
        this.webType = new LazyObject<>(() -> getWebType(context.getEnvironment()));
    }

    @Override
    public String getProperty(String name) {
        return context.getEnvironment().getProperty(name);
    }

    @Override
    public <T> T getBean(Class<T> type) {
        return context.getBean(type);
    }

    @Override
    public void register(AppBeanDefinition definition) {
        ConfigurableBeanFactory beanFactory = context.getBeanFactory();
        if (beanFactory instanceof BeanDefinitionRegistry) {
            BeanDefinitionRegistry registry = (BeanDefinitionRegistry) beanFactory;
            AbstractBeanDefinition def = new GenericBeanDefinition();
            def.setBeanClass(definition.getType());
            def.getPropertyValues().addPropertyValues(definition.getProperties());
            registry.registerBeanDefinition(definition.getName(), def);
        }
    }

    @Override
    public WebType getWebType() {
        return webType.get();
    }

    @Override
    public void subscribe(ConfigCenter configCenter) {
        refresher.subscribe(configCenter);
    }

    @Override
    public ConfigurableApplicationContext unwrap() {
        return context;
    }

    private WebType getWebType(ConfigurableEnvironment environment) {
        if (SpringUtils.isWeb(environment)) {
            if (SpringUtils.isJavaxServlet()) {
                return WebType.WEB_SERVLET_JAVAX;
            } else {
                return WebType.WEB_SERVLET_JAKARTA;
            }
        } else if (SpringUtils.isWebFlux(environment)) {
            return WebType.WEB_REACTIVE;
        }
        return WebType.NONE;
    }

}
