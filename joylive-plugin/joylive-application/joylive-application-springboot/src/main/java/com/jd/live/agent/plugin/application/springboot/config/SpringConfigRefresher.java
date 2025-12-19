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
package com.jd.live.agent.plugin.application.springboot.config;

import com.jd.live.agent.bootstrap.logger.Logger;
import com.jd.live.agent.bootstrap.logger.LoggerFactory;
import com.jd.live.agent.core.util.type.ClassDesc;
import com.jd.live.agent.core.util.type.FieldDesc;
import com.jd.live.agent.governance.config.RefreshConfig;
import com.jd.live.agent.governance.subscription.config.ConfigCenter;
import com.jd.live.agent.governance.subscription.config.Configurator;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.ClassUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static com.jd.live.agent.core.util.type.ClassUtils.describe;
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
     * Adds bean listeners to the given Configurator instance for all beans in the application context.
     * This method processes beans based on their annotations and configuration settings, adding listeners for
     * fields and methods annotated with {@link ConfigurationProperties} or {@link Value}.
     *
     * @param configurator The Configurator instance to which the listeners will be added.
     * @param config       The {@link RefreshConfig} instance used to check if the bean is enabled for configuration updates.
     */
    private void addBeanListener(Configurator configurator, RefreshConfig config) {
        Map<String, Object> beans = BeanFactoryUtils.beansOfTypeIncludingAncestors(context, Object.class);
        for (Map.Entry<String, Object> entry : beans.entrySet()) {
            String beanName = entry.getKey();
            Object bean = entry.getValue();
            if (config.isEnabled(beanName, bean)) {
                Class<?> beanClass = getOriginType(bean);
                ClassDesc describe = describe(beanClass);
                Set<Method> setters = new HashSet<>();
                boolean configurationPropertiesBean = processTypeConfigurationProperties(configurator, config, bean, describe);
                if (!configurationPropertiesBean) {
                    processMethodConfigurationProperties(configurator, config, bean, describe, setters);
                    processFieldValueAnnotation(configurator, config, bean, describe, setters);
                    processMethodValueAnnotation(configurator, config, bean, describe, setters);
                }
            }
        }
    }

    /**
     * Processes the {@link ConfigurationProperties} annotation at the class level for the given bean.
     * If the annotation is present, it adds a configuration properties listener for the bean.
     *
     * @param configurator The configurator to add listeners to.
     * @param config       The {@link RefreshConfig} instance used to check if the bean is enabled for configuration updates.
     * @param bean         The bean to process.
     * @param describe     The class description of the bean.
     * @return {@code true} if the annotation is found and processed, {@code false} otherwise.
     */
    private boolean processTypeConfigurationProperties(Configurator configurator, RefreshConfig config, Object bean, ClassDesc describe) {
        ConfigurationProperties typeAnnotation = describe.getType().getAnnotation(ConfigurationProperties.class);
        if (typeAnnotation != null) {
            addConfigurationPropertiesListener(configurator, config, bean, describe, typeAnnotation);
            return true;
        }
        return false;
    }

    /**
     * Processes the {@link ConfigurationProperties} annotation at the method level for the given bean.
     * If the annotation is present and the method is a valid bean method, it adds a configuration properties listener.
     *
     * @param configurator The configurator to add listeners to.
     * @param config       The {@link RefreshConfig} instance used to check if the bean is enabled for configuration updates.
     * @param bean         The bean to process.
     * @param describe     The class description of the bean.
     * @param methods      A set of methods already processed to avoid duplicates.
     */
    private void processMethodConfigurationProperties(Configurator configurator, RefreshConfig config, Object bean, ClassDesc describe, Set<Method> methods) {
        // only process the method in type with @Configuration annotation.
        Configuration configuration = describe.getType().getAnnotation(Configuration.class);
        if (configuration != null) {
            describe.getMethodList().forEach(method -> {
                if (!methods.contains(method)) {
                    ConfigurationProperties methodAnnotation = method.getAnnotation(ConfigurationProperties.class);
                    if (methodAnnotation != null && method.getReturnType() != void.class) {
                        Bean beanAnnotation = method.getAnnotation(Bean.class);
                        String beanName = getBeanName(beanAnnotation, method);
                        if (context.containsBean(beanName)) {
                            try {
                                methods.add(method);
                                Object targetBean = context.getBean(beanName);
                                addConfigurationPropertiesListener(configurator, config, targetBean, describe(bean.getClass()), methodAnnotation);
                            } catch (BeansException ignored) {
                            }
                        }
                    }
                }
            });
        }
    }

    /**
     * Processes fields annotated with {@link Value} for the given bean.
     * Adds listeners for fields with valid keys and setters.
     *
     * @param configurator The configurator to add listeners to.
     *                     @param config       The {@link RefreshConfig} instance used to check if the bean is enabled for configuration updates.
     * @param bean         The bean to process.
     * @param describe     The class description of the bean.
     * @param methods      A set of methods already processed to avoid duplicates.
     */
    private void processFieldValueAnnotation(Configurator configurator,
                                             RefreshConfig config,
                                             Object bean,
                                             ClassDesc describe,
                                             Set<Method> methods) {
        describe.getFieldList().forEach(field -> {
            if (isValid(field.getField())) {
                String key = getFieldKey(field);
                if (config.isEnabled(key)) {
                    Method setter = field.getSetter();
                    if (setter == null || methods.add(setter)) {
                        addFieldListener(configurator, config, bean, field, key);
                    }
                }
            }
        });
    }

    /**
     * Processes methods annotated with {@link Value} for the given bean.
     * Adds listeners for methods with valid keys and single parameters.
     *
     * @param configurator The configurator to add listeners to.
     * @param config       The {@link RefreshConfig} instance used to check if the bean is enabled for configuration updates.
     * @param bean         The bean to process.
     * @param describe     The class description of the bean.
     * @param methods      A set of methods already processed to avoid duplicates.
     */
    private void processMethodValueAnnotation(Configurator configurator,
                                              RefreshConfig config,
                                              Object bean,
                                              ClassDesc describe,
                                              Set<Method> methods) {
        describe.getMethodList().forEach(method -> {
            if (isValid(method)) {
                String key = getMethodKey(method);
                if (config.isEnabled(key) && methods.add(method)) {
                    addMethodListener(configurator, config, bean, method, key);
                }
            }
        });
    }

    /**
     * Adds a configuration properties listener for the given bean and annotation.
     * Processes all fields of the bean and adds listeners for each field with a valid key.
     *
     * @param configurator The configurator to add listeners to.
     *                     @param config       The {@link RefreshConfig} instance used to check if the bean is enabled for configuration updates.
     * @param bean         The bean to process.
     * @param describe     The class description of the bean.
     * @param annotation   The {@link ConfigurationProperties} annotation.
     */
    private void addConfigurationPropertiesListener(Configurator configurator,
                                                    RefreshConfig config,
                                                    Object bean,
                                                    ClassDesc describe,
                                                    ConfigurationProperties annotation) {
        if (annotation != null) {
            String prefix = annotation.prefix().isEmpty() ? annotation.value() : annotation.prefix();
            describe.getFieldList().forEach(field -> {
                if (isValid(field.getField())) {
                    String key = getFieldKey(field, prefix, true);
                    if (config.isEnabled(key)) {
                        addFieldListener(configurator, config, bean, field, key);
                    }
                }
            });
        }
    }

    /**
     * Adds a listener for a specific field of the bean.
     * Updates the field value when a configuration update event occurs.
     *
     * @param configurator The configurator to add listeners to.
     * @param config       The {@link RefreshConfig} instance used to check if the bean is enabled for configuration updates.
     * @param bean         The bean containing the field.
     * @param field        The field to listen for updates.
     * @param key          The configuration key associated with the field.
     */
    private void addFieldListener(Configurator configurator, RefreshConfig config, Object bean, FieldDesc field, String key) {
        configurator.addListener(key, event -> {
            if (event.getType() == UPDATE) {
                try {
                    Object value = convert(event.getValue(), field.getType());
                    field.set(bean, value);
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

    /**
     * Adds a listener for a specific method of the bean.
     * Invokes the method with the updated value when a configuration update event occurs.
     *
     * @param configurator The configurator to add listeners to.
     * @param config       The {@link RefreshConfig} instance used to check if the bean is enabled for configuration updates.
     * @param bean         The bean containing the method.
     * @param method       The method to listen for updates.
     * @param key          The configuration key associated with the method.
     */
    private void addMethodListener(Configurator configurator, RefreshConfig config, Object bean, Method method, String key) {
        configurator.addListener(key, event -> {
            if (event.getType() == UPDATE) {
                try {
                    Object value = convert(event.getValue(), method.getParameters()[0].getType());
                    method.invoke(bean, value);
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

    private boolean isValid(Method method) {
        return !Modifier.isStatic(method.getModifiers())
                && method.getParameterCount() == 1
                && method.getReturnType() == void.class;
    }

    private boolean isValid(Field field) {
        return !Modifier.isStatic(field.getModifiers()) && !Modifier.isFinal(field.getModifiers());
    }

    /**
     * Retrieves the bean name based on the provided {@link Bean} annotation and method.
     * If the {@link Bean} annotation specifies a non-empty name or value, it is used as the bean name.
     * If no valid name is found in the annotation, the method name is used as the bean name.
     *
     * @param bean   The {@link Bean} annotation associated with the method. Can be {@code null}.
     * @param method The method for which the bean name is being retrieved. Must not be {@code null}.
     * @return The bean name derived from the annotation or the method name if no valid name is found in the annotation.
     */
    private String getBeanName(Bean bean, Method method) {
        String beanName = null;
        if (bean != null) {
            String[] names = bean.name();
            names = names.length > 0 ? names : bean.value();
            for (String name : names) {
                if (!name.isEmpty()) {
                    beanName = name;
                    break;
                }
            }
        }
        if (beanName == null) {
            beanName = method.getName();
        }
        return beanName;
    }

    private Class<?> getOriginType(Object bean) {
        return ClassUtils.getUserClass(bean);
    }

    /**
     * Returns the configuration key for the given field. Delegates to {@link #getFieldKey(FieldDesc, String, boolean)} with {@code prefix} as {@code null} and {@code isConfig} as {@code false}.
     *
     * @param field The field to retrieve the key for. Must not be {@code null}.
     * @return The configuration key, or {@code null} if no key is found.
     */
    private String getFieldKey(FieldDesc field) {
        return getFieldKey(field, null, false);
    }

    /**
     * Retrieves the configuration key for the given field. Uses the {@link Value} annotation's value as the key.
     * If the key is empty or {@code null}, and {@code isConfig} is {@code true}, the field's name is used as the key.
     * The key is prefixed with the provided {@code prefix} if it is not {@code null} or empty.
     *
     * @param field    The field to retrieve the key for. Must not be {@code null}.
     * @param prefix   The prefix to prepend to the key. Can be {@code null} or empty.
     * @param isConfig If {@code true}, falls back to the field name if no key is found in the annotation.
     * @return The configuration key, prefixed if applicable.
     */
    private String getFieldKey(FieldDesc field, String prefix, boolean isConfig) {
        String name = getKey(field.getAnnotation(Value.class));
        name = (name == null || name.isEmpty()) && isConfig ? field.getName() : name;
        if (prefix == null || prefix.isEmpty()) {
            return name;
        } else if (prefix.equals(".")) {
            return prefix + name;
        } else {
            return prefix + "." + name;
        }
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
