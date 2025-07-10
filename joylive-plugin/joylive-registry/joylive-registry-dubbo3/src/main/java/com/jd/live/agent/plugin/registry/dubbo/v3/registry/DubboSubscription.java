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
package com.jd.live.agent.plugin.registry.dubbo.v3.registry;

import com.jd.live.agent.governance.registry.ServiceId;
import lombok.Getter;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.registry.NotifyListener;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Represents a subscription for Dubbo notifications.
 * Maintains a set of listeners to be notified of changes.
 */
public class DubboSubscription implements AutoCloseable {

    @Getter
    private final String id;

    @Getter
    private final ServiceId serviceId;

    @Getter
    private final URL url;

    private final Map<NotifyListener, DubboNotifyListener> listeners = new ConcurrentHashMap<>();

    public DubboSubscription(ServiceId serviceId, URL url) {
        this(serviceId.getUniqueName(), serviceId, url);
    }

    public DubboSubscription(String id, ServiceId serviceId, URL url) {
        this.id = id;
        this.serviceId = serviceId;
        this.url = url;
    }

    /**
     * Adds a notification listener.
     *
     * @return true if the listener was newly added
     */
    public boolean addListener(DubboNotifyListener listener) {
        return listeners.put(listener.getDelegate(), listener) == null;
    }

    /**
     * Removes a notification listener.
     *
     * @return the removed listener, or null if not found
     */
    public DubboNotifyListener removeListener(NotifyListener listener) {
        return listeners.remove(listener);
    }

    @Override
    public void close() {
        listeners.forEach((key, listener) -> listener.close());
        listeners.clear();
    }

    public boolean isEmpty() {
        return listeners.isEmpty();
    }

}
