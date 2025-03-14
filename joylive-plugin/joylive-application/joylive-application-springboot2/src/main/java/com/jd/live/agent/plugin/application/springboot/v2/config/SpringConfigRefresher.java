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
package com.jd.live.agent.plugin.application.springboot.v2.config;

import com.jd.live.agent.bootstrap.logger.Logger;
import com.jd.live.agent.bootstrap.logger.LoggerFactory;
import com.jd.live.agent.core.util.type.ClassDesc;
import com.jd.live.agent.core.util.type.ClassUtils;
import com.jd.live.agent.core.util.type.FieldDesc;
import com.jd.live.agent.governance.config.RefreshConfig;
import com.jd.live.agent.governance.subscription.config.ConfigCenter;
import com.jd.live.agent.governance.subscription.config.Configurator;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static com.jd.live.agent.governance.subscription.config.ConfigEvent.EventType.UPDATE;
import static com.jd.live.agent.governance.subscription.config.ConfigListener.SYSTEM_ALL;

/**
 * A utility class that helps refresh Spring configuration.
 */
public class SpringConfigRefresher {

    private static final Logger logger = LoggerFactory.getLogger(SpringConfigRefresher.class);

    private final ConfigurableApplicationContext context;

    /**
     * The refresher responsible for refreshing the environment.
     */
    private final ConfigRefresher refresher;

    /**
     * Constructs a new SpringApplicationContext instance.
     *
     * @param context The Spring application context.
     */
    public SpringConfigRefresher(ConfigurableApplicationContext context) {
        this.context = context;
        this.refresher = SpringRefresherFactory.getRefresher(context);
    }

    /**
     * Subscribes the application context to the given ConfigCenter instance.
     *
     * @param configCenter The ConfigCenter instance to which the application context will be subscribed.
     */
    public void subscribe(ConfigCenter configCenter) {
        configCenter.ifPresent(configurator -> {
            RefreshConfig config = configCenter.getConfig().getRefresh();
            if (config.isEnvironmentEnabled()) {
                addEnvironmentListener(configurator);
            }
            if (config.isBeanEnabled()) {
                addBeanListener(configurator, config);
            }
        });
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
            if (config.isEnabled(beanName, bean)) {
                ClassDesc describe = ClassUtils.describe(bean.getClass());
                Set<String> configurationPropertiesFields = new HashSet<>();
                processConfigurationProperties(configurator, bean, describe, configurationPropertiesFields);
                Set<Method> setters = new HashSet<>();
                addFieldListener(configurator, bean, describe, setters, configurationPropertiesFields);
                addMethodListener(configurator, bean, describe, setters, configurationPropertiesFields);
            }
        }
    }

    private void processConfigurationProperties(Configurator configurator, Object bean, ClassDesc describe,
                                                Set<String> configurationPropertiesFields) {
        ConfigurationProperties classAnnotation = org.springframework.util.ClassUtils
                .getUserClass(bean).getAnnotation(ConfigurationProperties.class);
        if (classAnnotation != null) {
            String prefix = classAnnotation.prefix();
            if (prefix.isEmpty()) {
                prefix = classAnnotation.value();
            }
            handleConfigurationPropertiesBean(configurator, bean, describe, prefix, configurationPropertiesFields);
        }

        describe.getMethodList().forEach(method -> {
            ConfigurationProperties methodAnnotation = method.getAnnotation(ConfigurationProperties.class);
            if (methodAnnotation != null && method.getReturnType() != void.class) {
                String prefix = methodAnnotation.prefix();
                if (prefix.isEmpty()) {
                    prefix = methodAnnotation.value();
                }
                try {
                    Bean beanAnnotation = method.getAnnotation(Bean.class);
                    String methodBeanName = null;
                    if (beanAnnotation != null) {
                        String[] names = beanAnnotation.name();
                        if (names.length > 0 && !names[0].isEmpty()) {
                            methodBeanName = names[0];
                        } else {
                            String[] values = beanAnnotation.value();
                            if (values.length > 0 && !values[0].isEmpty()) {
                                methodBeanName = values[0];
                            }
                        }
                    }
                    if (methodBeanName == null) {
                        methodBeanName = method.getName();
                    }
                    Object methodBean = context.containsBean(methodBeanName)
                            ? context.getBean(methodBeanName)
                            : method.invoke(bean);
                    if (methodBean != null) {
                        ClassDesc methodBeanDesc = ClassUtils.describe(methodBean.getClass());
                        handleConfigurationPropertiesBean(configurator, methodBean, methodBeanDesc, prefix, new HashSet<>());
                    }
                } catch (Exception e) {
                    logger.error("Failed to process @ConfigurationProperties on method " + method.getName(), e);
                }
            }
        });
    }

    private void handleConfigurationPropertiesBean(Configurator configurator, Object bean, ClassDesc describe,
                                                   String prefix, Set<String> configurationPropertiesFields) {
        if (prefix == null || prefix.isEmpty()) {
            return;
        }

        describe.getFieldList().forEach(field -> {
            String propertyPath = buildPropertyPath(prefix, field.getName());
            Method setter = field.getSetter();
            if (setter != null) {
                configurationPropertiesFields.add(field.getName());
                configurator.addListener(propertyPath, event -> {
                    if (event.getType() == UPDATE) {
                        try {
                            field.set(bean, convert(event.getValue(), field.getType()));
                            return true;
                        } catch (Throwable e) {
                            logger.error("Failed to update config " + propertyPath +
                                    ", by set field " + field.getName() +
                                    " of class " + bean.getClass(), e);
                        }
                    }
                    return false;
                });
            }
        });
    }

    private String buildPropertyPath(String prefix, String fieldName) {
        return prefix.endsWith(".") ? prefix + fieldName : prefix + "." + fieldName;
    }

    /**
     * Adds a field listener to the given ConfigCenter instance for the specified bean and field.
     *
     * @param configurator The Configurator instance to which the listener will be added.
     * @param bean         The bean for which the field listener will be added.
     * @param describe     The ClassDesc object describing the bean's class.
     * @param setters      A set of methods that are already registered as setters.
     * @param configurationPropertiesFields A set of fields thar are already listened
     */
    private void addFieldListener(Configurator configurator, Object bean, ClassDesc describe,
                                  Set<Method> setters, Set<String> configurationPropertiesFields) {
        describe.getFieldList().forEach(field -> {
            if (!configurationPropertiesFields.contains(field.getName())) {
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
                                logger.error("Failed to update config " + key +
                                        " by set field " + field.getName() +
                                        " of class " + bean.getClass(), e);
                            }
                        }
                        return false;
                    });
                }
            }
        });
    }

    /**
     * Adds a method listener to the given ConfigCenter instance for the specified bean and method.
     *
     * @param configurator The Configurator instance to which the listener will be added.
     * @param bean         The bean for which the method listener will be added.
     * @param describe     The ClassDesc object describing the bean's class.
     * @param setters      A set of methods that are already registered as setters.
     * @param configurationPropertiesFields A set of fields thar are already listened
     */
    private void addMethodListener(Configurator configurator, Object bean, ClassDesc describe,
                                   Set<Method> setters, Set<String> configurationPropertiesFields) {
        describe.getMethodList().forEach(method -> {
            if (!setters.contains(method) && method.getParameterCount() == 1) {
                String fieldName = getFieldNameFromSetter(method);
                if (fieldName == null || !configurationPropertiesFields.contains(fieldName)) {
                    String key = getMethodKey(method);
                    if (key != null) {
                        configurator.addListener(key, event -> {
                            if (event.getType() == UPDATE) {
                                try {
                                    method.invoke(bean, convert(event.getValue(), method.getParameters()[0].getType()));
                                    return true;
                                } catch (Throwable e) {
                                    logger.error("Failed to update config " + key +
                                            " by invoking method " + method.getName() +
                                            " of class " + bean.getClass(), e);
                                }
                            }
                            return false;
                        });
                    }
                }
            }
        });
    }

    private String getFieldNameFromSetter(Method method) {
        String name = method.getName();
        if (name.startsWith("set") && name.length() > 3) {
            return Character.toLowerCase(name.charAt(3)) + name.substring(4);
        }
        return null;
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

}
