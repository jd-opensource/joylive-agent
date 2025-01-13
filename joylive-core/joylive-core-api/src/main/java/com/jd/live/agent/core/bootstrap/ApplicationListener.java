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

import com.jd.live.agent.core.extension.annotation.Extensible;

import java.util.ArrayList;
import java.util.List;

/**
 * An interface for listening to application events.
 *
 * @since 1.6.0
 */
@Extensible("ApplicationListener")
public interface ApplicationListener {

    int ORDER_BOOTSTRAP = 0;

    int ORDER_POLICY_PREPARATION = 1;

    String COMPONENT_APPLICATION_LISTENER = "applicationListener";

    /**
     * Called when the environment is prepared and ready for use.
     *
     * @param context     The application context.
     * @param environment The application environment.
     */
    void onEnvironmentPrepared(ApplicationBootstrapContext context, ApplicationEnvironment environment);

    /**
     * Called when the application has started.
     *
     * @param context The application context.
     */
    void onStarted(ApplicationContext context);

    /**
     * Called when the application is fully initialized and ready to serve requests.
     *
     * @param context The application context.
     */
    void onReady(ApplicationContext context);

    /**
     * Called when the application is stopping.
     *
     * @param context The application context.
     */
    void onStop(ApplicationContext context);

    /**
     * A default implementation of the ApplicationListener interface that does nothing.
     *
     * @since 1.6.0
     */
    class ApplicationListenerAdapter implements ApplicationListener {

        @Override
        public void onEnvironmentPrepared(ApplicationBootstrapContext context, ApplicationEnvironment environment) {
            // Do nothing
        }

        @Override
        public void onStarted(ApplicationContext context) {
            // Do nothing
        }

        @Override
        public void onReady(ApplicationContext context) {
            // Do nothing
        }

        @Override
        public void onStop(ApplicationContext context) {

        }
    }

    /**
     * A wrapper class for multiple ApplicationListeners.
     *
     * @since 1.6.0
     */
    class ApplicationListenerWrapper implements ApplicationListener {

        private final List<ApplicationListener> listeners;

        public ApplicationListenerWrapper(List<ApplicationListener> listeners) {
            this.listeners = listeners == null ? new ArrayList<>(0) : listeners;
        }

        @Override
        public void onEnvironmentPrepared(ApplicationBootstrapContext context, ApplicationEnvironment environment) {
            for (ApplicationListener listener : listeners) {
                listener.onEnvironmentPrepared(context, environment);
            }
        }

        @Override
        public void onStarted(ApplicationContext context) {
            for (ApplicationListener listener : listeners) {
                listener.onStarted(context);
            }
        }

        @Override
        public void onReady(ApplicationContext context) {
            for (ApplicationListener listener : listeners) {
                listener.onReady(context);
            }
        }

        @Override
        public void onStop(ApplicationContext context) {
            for (ApplicationListener listener : listeners) {
                listener.onStop(context);
            }
        }
    }
}
