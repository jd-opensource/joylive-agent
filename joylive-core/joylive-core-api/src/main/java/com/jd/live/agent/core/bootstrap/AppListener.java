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

import java.util.LinkedList;
import java.util.List;

/**
 * An interface for listening to application events.
 *
 * @since 1.6.0
 */
@Extensible("ApplicationListener")
public interface AppListener {

    int ORDER_BOOTSTRAP = 0;

    int ORDER_POLICY_PREPARATION = ORDER_BOOTSTRAP + 1;

    int ORDER_OPEN_API = ORDER_POLICY_PREPARATION + 1;

    int ORDER_MCP = ORDER_OPEN_API + 1;

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
     * Called when application context is prepared
     *
     * @param context Application context
     */
    void onContextPrepared(AppContext context);

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
        public void onContextPrepared(AppContext context) {

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

    class CompositeAppListener implements AppBooter {

        protected final List<AppListener> listeners;

        public CompositeAppListener(List<AppListener> listeners) {
            this.listeners = listeners;
        }

        @Override
        public void onLoading(ClassLoader classLoader, Class<?> mainClass) {
            listeners.forEach(listener -> listener.onLoading(classLoader, mainClass));
        }

        @Override
        public void onEnvironmentPrepared(AppBootstrapContext context, AppEnvironment environment) {
            listeners.forEach(listener -> listener.onEnvironmentPrepared(context, environment));
        }

        @Override
        public void onContextPrepared(AppContext context) {
            listeners.forEach(listener -> listener.onContextPrepared(context));
        }

        @Override
        public void onStarted(AppContext context) {
            listeners.forEach(listener -> listener.onStarted(context));
        }

        @Override
        public void onReady(AppContext context) {
            listeners.forEach(listener -> listener.onReady(context));
        }

        @Override
        public void onCLose(AppContext context) {
            listeners.forEach(listener -> listener.onCLose(context));
        }

        public static CompositeAppListener composite(AppListener listener, List<? extends AppListener> listeners) {
            List<AppListener> result = new LinkedList<>();
            if (listener != null) {
                result.add(listener);
            }
            if (listeners != null) {
                result.addAll(listeners);
            }
            return new CompositeAppListener(result);
        }

    }

}
