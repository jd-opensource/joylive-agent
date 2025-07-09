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
public interface AppListener {

    int ORDER_BOOTSTRAP = 0;

    int ORDER_POLICY_PREPARATION = 1;

    String COMPONENT_APPLICATION_LISTENER = "applicationListener";

    /**
     * Invoked when a ClassLoader is about to load a class.
     *
     * @param classLoader The ClassLoader that is about to load a class.
     * @param mainClass   The main class that is being loaded.
     */
    void onLoading(ClassLoader classLoader, Class<?> mainClass);

    /**
     * Called when the environment is prepared and ready for use.
     *
     * @param context     The application context.
     * @param environment The application environment.
     */
    void onEnvironmentPrepared(AppBootstrapContext context, AppEnvironment environment);

    /**
     * Called when the application has started.
     *
     * @param context The application context.
     */
    void onStarted(AppContext context);

    /**
     * Called when the application is fully initialized and ready to serve requests.
     *
     * @param context The application context.
     */
    void onReady(AppContext context);

    /**
     * Called when the application is stopping.
     *
     * @param context The application context.
     */
    void onCLose(AppContext context);

    /**
     * A default implementation of the ApplicationListener interface that does nothing.
     *
     * @since 1.6.0
     */
    class AppListenerAdapter implements AppListener {

        @Override
        public void onLoading(ClassLoader classLoader, Class<?> mainClass) {
            // Do nothing
        }

        @Override
        public void onEnvironmentPrepared(AppBootstrapContext context, AppEnvironment environment) {
            // Do nothing
        }

        @Override
        public void onStarted(AppContext context) {
            // Do nothing
        }

        @Override
        public void onReady(AppContext context) {
            // Do nothing
        }

        @Override
        public void onCLose(AppContext context) {
            // Do nothing
        }
    }

    /**
     * A wrapper class for multiple ApplicationListeners.
     *
     * @since 1.6.0
     */
    class AppListenerWrapper implements AppListener {

        private final List<AppListener> listeners;

        public AppListenerWrapper(List<AppListener> listeners) {
            this.listeners = listeners == null ? new ArrayList<>(0) : listeners;
        }

        @Override
        public void onLoading(ClassLoader classLoader, Class<?> mainClass) {
            for (AppListener listener : listeners) {
                listener.onLoading(classLoader, mainClass);
            }
        }

        @Override
        public void onEnvironmentPrepared(AppBootstrapContext context, AppEnvironment environment) {
            for (AppListener listener : listeners) {
                listener.onEnvironmentPrepared(context, environment);
            }
        }

        @Override
        public void onStarted(AppContext context) {
            for (AppListener listener : listeners) {
                listener.onStarted(context);
            }
        }

        @Override
        public void onReady(AppContext context) {
            for (AppListener listener : listeners) {
                listener.onReady(context);
            }
        }

        @Override
        public void onCLose(AppContext context) {
            for (AppListener listener : listeners) {
                listener.onCLose(context);
            }
        }
    }
}
