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
package com.jd.live.agent.core.bootstrap;

/**
 * Represents the application context, which holds all the beans and their dependencies.
 *
 * @since 1.6.0
 */
public interface AppContext {

    /**
     * Gets configuration property.
     *
     * @param name property name
     * @return property value
     */
    String getProperty(String name);

    /**
     * Gets bean by type.
     *
     * @param type bean type
     * @return bean instance
     */
    <T> T getBean(Class<T> type);

    /**
     * Registers a bean definition in the application context.
     *
     * @param definition the bean definition to register
     */
    void register(AppBeanDefinition definition);

    /**
     * Gets the web technology type of the application.
     *
     * @return the web type indicating the underlying web technology stack
     */
    WebType getWebType();

    /**
     * Unwraps and returns the underlying native application context object.
     *
     * @return the underlying application context implementation
     */
    Object unwrap();
}

