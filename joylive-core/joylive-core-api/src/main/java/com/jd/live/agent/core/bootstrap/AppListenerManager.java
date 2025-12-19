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

import com.jd.live.agent.core.bootstrap.AppListener.CompositeAppListener;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * A wrapper class for multiple ApplicationListeners.
 *
 * @since 1.6.0
 */
public class AppListenerManager extends CompositeAppListener implements AppListenerSupervisor {

    public AppListenerManager(List<AppListener> listeners) {
        super(listeners == null ? new CopyOnWriteArrayList<>() : new CopyOnWriteArrayList<>(listeners));
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
