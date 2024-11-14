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
package com.jd.live.agent.core.service.sync;

import com.jd.live.agent.core.config.ConfigEvent;
import com.jd.live.agent.core.config.ConfigEvent.EventType;
import com.jd.live.agent.core.config.ConfigListener;
import com.jd.live.agent.core.service.ConfigService;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * An abstract class for a configuration synchronization service that extends the AbstractSyncer class and implements the ConfigService interface.
 *
 * @param <T> The type of the source data.
 * @param <M> The type of the target data.
 */
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
    protected boolean update(T value, M digest) {
        for (ConfigListener listener : listeners) {
            if (!listener.onUpdate(create(value))) {
                return false;
            }
        }
        return true;
    }

    /**
     * Creates a new Configuration object with the specified value, description, and watcher.
     *
     * @param value The value of the configuration.
     * @return A newly created Configuration object.
     */
    protected ConfigEvent create(T value) {
        return ConfigEvent.builder().type(EventType.UPDATE_ITEM).value(value).description(getType()).watcher(getName()).build();
    }

}
