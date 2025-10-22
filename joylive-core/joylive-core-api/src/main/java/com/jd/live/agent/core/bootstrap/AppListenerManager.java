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

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * A wrapper class for multiple ApplicationListeners.
 *
 * @since 1.6.0
 */
public class AppListenerManager implements AppListenerSupervisor {

    private final List<AppListener> listeners;

    public AppListenerManager(List<AppListener> listeners) {
        this.listeners = listeners == null ? new CopyOnWriteArrayList<>() : new CopyOnWriteArrayList<>(listeners);
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

    @Override
    public void add(AppListener listener) {
        if (listeners != null) {
            listeners.add(listener);
        }
    }

    @Override
    public void addFirst(AppListener listener) {
        if (listeners != null) {
            listeners.add(0, listener);
        }
    }

    @Override
    public void remove(AppListener listener) {
        if (listeners != null) {
            listeners.remove(listener);
        }
    }
}
