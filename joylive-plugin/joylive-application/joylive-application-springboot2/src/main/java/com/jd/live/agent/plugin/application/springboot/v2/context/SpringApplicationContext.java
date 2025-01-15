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

import com.jd.live.agent.core.bootstrap.ApplicationContext;
import org.springframework.cloud.context.environment.EnvironmentChangeEvent;
import org.springframework.context.ConfigurableApplicationContext;

import java.util.HashSet;

/**
 * An implementation of the ApplicationContext interface for Spring-based applications.
 */
public class SpringApplicationContext implements ApplicationContext {

    /**
     * The type of event that is published when the environment changes.
     */
    private static final String TYPE_ENVIRONMENT_CHANGE_EVENT = "org.springframework.cloud.context.environment.EnvironmentChangeEvent";

    /**
     * The refresher responsible for refreshing the environment.
     */
    private final Refresher refresher;

    /**
     * Constructs a new SpringApplicationContext instance.
     *
     * @param context The Spring application context.
     */
    public SpringApplicationContext(ConfigurableApplicationContext context) {
        this.refresher = getRefresher(context);
    }

    @Override
    public void refreshEnvironment() {
        refresher.refresh();
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
