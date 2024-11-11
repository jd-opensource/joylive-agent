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
package com.jd.live.agent.core.service;

import com.jd.live.agent.core.config.ConfigListener;
import com.jd.live.agent.core.config.Configuration;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public abstract class AbstractConfigSyncer<T, M> extends AbstractSyncer<T, M> implements ConfigService {

    protected final List<ConfigListener> listeners = new CopyOnWriteArrayList<>();

    @Override
    public void addListener(String type, ConfigListener listener) {
        if (getType().equals(type) && listener != null) {
            listeners.add(listener);
        }
    }

    @Override
    public void removeListener(String type, ConfigListener listener) {
        if (getType().equals(type) && listener != null) {
            listeners.remove(listener);
        }
    }

    @Override
    protected boolean updateOnce(T value, M digest) {
        listeners.forEach(listener -> listener.onUpdate(create(value)));
        return true;
    }

    /**
     * Creates a new Configuration object with the specified value, description, and watcher.
     *
     * @param value The value of the configuration.
     * @return A newly created Configuration object.
     */
    protected Configuration create(T value) {
        return Configuration.builder().value(value).description(getType()).watcher(getName()).build();
    }

}
