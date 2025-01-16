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
package com.jd.live.agent.plugin.application.springboot.v2.context;

import com.jd.live.agent.bootstrap.logger.Logger;
import com.jd.live.agent.bootstrap.logger.LoggerFactory;
import com.jd.live.agent.core.util.type.ClassDesc;
import com.jd.live.agent.core.util.type.ClassUtils;
import com.jd.live.agent.core.util.type.FieldDesc;
import com.jd.live.agent.governance.bootstrap.ConfigurableAppContext;
import com.jd.live.agent.governance.config.RefreshConfig;
import com.jd.live.agent.governance.subscription.config.ConfigCenter;
import com.jd.live.agent.governance.subscription.config.Configurator;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.environment.EnvironmentChangeEvent;
import org.springframework.context.ConfigurableApplicationContext;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static com.jd.live.agent.governance.subscription.config.ConfigEvent.EventType.UPDATE;
import static com.jd.live.agent.governance.subscription.config.ConfigListener.SYSTEM_ALL;

/**
 * An implementation of the ApplicationContext interface for Spring-based applications.
 */
public class SpringAppContext implements ConfigurableAppContext {

    private static final Logger logger = LoggerFactory.getLogger(SpringAppContext.class);

    /**
     * The type of event that is published when the environment changes.
     */
    private static final String TYPE_ENVIRONMENT_CHANGE_EVENT = "org.springframework.cloud.context.environment.EnvironmentChangeEvent";

    private final ConfigurableApplicationContext context;

    /**
     * The refresher responsible for refreshing the environment.
     */
    private final Refresher refresher;

    /**
     * Constructs a new SpringApplicationContext instance.
     *
     * @param context The Spring application context.
     */
    public SpringAppContext(ConfigurableApplicationContext context) {
        this.context = context;
        this.refresher = getRefresher(context);
    }

    @Override
    public void subscribe(ConfigCenter configCenter) {
        Configurator configurator = configCenter.getConfigurator();
        if (configurator != null) {
            RefreshConfig config = configCenter.getConfig().getRefresh();
            if (config.isEnvironmentEnabled()) {
                addEnvironmentListener(configurator);
            }
            if (config.isBeanEnabled()) {
                addBeanListener(configurator, config);
            }
        }
    }

    public ConfigurableApplicationContext getContext() {
        return context;
    }

    /**
     * Adds an environment listener to the given Configurator instance.
     *
     * @param configurator The Configurator instance to which the listener will be added.
     */
    private void addEnvironmentListener(Configurator configurator) {
        configurator.addListener(SYSTEM_ALL, e -> {
            refresher.refresh();
            return true;
        });
    }

    /**
     * Adds bean listeners to the given ConfigCenter instance for all beans in the application context.
     *
     * @param configurator The Configurator instance to which the listener will be added.
     */
    private void addBeanListener(Configurator configurator, RefreshConfig config) {
        Map<String, Object> beans = BeanFactoryUtils.beansOfTypeIncludingAncestors(context, Object.class);
        for (Map.Entry<String, Object> entry : beans.entrySet()) {
            String beanName = entry.getKey();
            Object bean = entry.getValue();
            if (config.isEnabled(beanName, bean.getClass())) {
                ClassDesc describe = ClassUtils.describe(bean.getClass());
                Set<Method> setters = new HashSet<>();
                addFieldListener(configurator, bean, describe, setters);
                addMethodListener(configurator, bean, describe, setters);
            }
        }
    }

    /**
     * Adds a method listener to the given ConfigCenter instance for the specified bean and method.
     *
     * @param configurator The Configurator instance to which the listener will be added.
     * @param bean         The bean for which the method listener will be added.
     * @param describe     The ClassDesc object describing the bean's class.
     * @param setters      A set of methods that are already registered as setters.
     */
    private void addMethodListener(Configurator configurator, Object bean, ClassDesc describe, Set<Method> setters) {
        describe.getMethodList().forEach(method -> {
            if (!setters.contains(method) && method.getParameterCount() == 1) {
                String key = getMethodKey(method);
                if (key != null) {
                    configurator.addListener(key, event -> {
                        if (event.getType() == UPDATE) {
                            try {
                                method.invoke(bean, convert(event.getValue(), method.getParameters()[0].getType()));
                                return true;
                            } catch (Throwable e) {
                                logger.error("Failed to update config " + key + " by invoking method " + method.getName() + " of class " + bean.getClass(), e);
                            }
                        }
                        return false;
                    });
                }
            }
        });
    }

    /**
     * Adds a field listener to the given ConfigCenter instance for the specified bean and field.
     *
     * @param configurator The Configurator instance to which the listener will be added.
     * @param bean         The bean for which the field listener will be added.
     * @param describe     The ClassDesc object describing the bean's class.
     * @param setters      A set of methods that are already registered as setters.
     */
    private void addFieldListener(Configurator configurator, Object bean, ClassDesc describe, Set<Method> setters) {
        describe.getFieldList().forEach(field -> {
            String key = getFieldKey(field);
            if (key != null) {
                Method setter = field.getSetter();
                if (setter != null) {
                    setters.add(setter);
                }
                configurator.addListener(key, event -> {
                    if (event.getType() == UPDATE) {
                        try {
                            field.set(bean, convert(event.getValue(), field.getType()));
                            return true;
                        } catch (Throwable e) {
                            logger.error("Failed to update config " + key + ", by set field " + field.getName() + " of class " + bean.getClass(), e);
                        }
                    }
                    return false;
                });
            }
        });
    }

    /**
     * Returns the configuration key associated with the given field.
     *
     * @param field The field for which the configuration key is to be retrieved.
     * @return The configuration key, or null if no key is found.
     */
    private String getFieldKey(FieldDesc field) {
        return getKey(field.getAnnotation(Value.class));
    }

    /**
     * Returns the configuration key associated with the given method.
     *
     * @param method The method for which the configuration key is to be retrieved.
     * @return The configuration key, or null if no key is found.
     */
    private String getMethodKey(Method method) {
        Value value = method.getAnnotation(Value.class);
        if (value != null) {
            return getKey(value);
        }
        value = method.getParameters()[0].getAnnotation(Value.class);
        return getKey(value);
    }

    /**
     * Extracts the configuration key from the given Value annotation.
     *
     * @param value The Value annotation from which the configuration key is to be extracted.
     * @return The configuration key, or null if no key is found.
     */
    private String getKey(Value value) {
        String key = value == null ? null : value.value();
        if (key != null) {
            if (key.startsWith("${") && key.endsWith("}")) {
                key = key.substring(2, key.length() - 1);
                int index = key.indexOf(":");
                if (index > 0) {
                    key = key.substring(0, index);
                }
            }
        }
        return key;
    }

    /**
     * Converts the given value to the specified type using the ConversionService.
     *
     * @param value The value to be converted.
     * @param type  The target type to which the value is to be converted.
     * @return The converted value.
     */
    private Object convert(Object value, Class<?> type) {
        return context.getEnvironment().getConversionService().convert(value, type);
    }

    /**
     * Gets the refresher instance based on the given Spring application context.
     *
     * @param context The Spring application context.
     * @return The refresher instance.
     */
    private Refresher getRefresher(ConfigurableApplicationContext context) {
        // avoid none EnvironmentChangeEvent class.
        try {
            ClassLoader classLoader = context.getClass().getClassLoader();
            classLoader.loadClass(TYPE_ENVIRONMENT_CHANGE_EVENT);
            return new SpringCloudRefresher(context);
        } catch (Throwable e) {
            return new SpringBootRefresher();
        }
    }

    /**
     * An interface representing a refresher that can refresh the environment.
     */
    private interface Refresher {
        /**
         * Refreshes the environment.
         */
        void refresh();
    }

    /**
     * A refresher implementation for Spring Boot applications.
     */
    private static class SpringBootRefresher implements Refresher {

        SpringBootRefresher() {
        }

        @Override
        public void refresh() {
        }
    }

    /**
     * A refresher implementation for Spring Cloud applications.
     */
    private static class SpringCloudRefresher implements Refresher {

        private final ConfigurableApplicationContext context;

        SpringCloudRefresher(ConfigurableApplicationContext context) {
            this.context = context;
        }

        @Override
        public void refresh() {
            // refresh is slow.
            context.publishEvent(new EnvironmentChangeEvent(context, new HashSet<>()));
        }
    }

}
