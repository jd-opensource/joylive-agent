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
package com.jd.live.agent.plugin.application.springboot.v2.listener;

import com.jd.live.agent.core.bootstrap.AppListener;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * A utility class for managing a list of AppListeners.
 */
public class InnerListener {

    /**
     * A thread-safe list of AppListeners.
     */
    private final static List<AppListener> listeners = new CopyOnWriteArrayList<>();

    /**
     * Adds an AppListener to the list of listeners.
     *
     * @param listener The AppListener to be added.
     */
    public static void add(AppListener listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }

    /**
     * Removes an AppListener from the list of listeners.
     *
     * @param listener The AppListener to be removed.
     */
    public static void remove(AppListener listener) {
        if (listener != null) {
            listeners.remove(listener);
        }
    }

    /**
     * Iterates over the list of AppListeners and performs the given action on each listener.
     *
     * @param consumer The action to be performed on each AppListener.
     */
    public static void foreach(Consumer<AppListener> consumer) {
        if (consumer != null) {
            listeners.forEach(consumer);
        }
    }
}

