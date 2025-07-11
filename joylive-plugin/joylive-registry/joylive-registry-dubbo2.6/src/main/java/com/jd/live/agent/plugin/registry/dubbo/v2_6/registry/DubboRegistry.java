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
package com.jd.live.agent.plugin.registry.dubbo.v2_6.registry;

import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.registry.NotifyListener;
import com.alibaba.dubbo.registry.Registry;
import com.jd.live.agent.governance.registry.*;
import com.jd.live.agent.governance.registry.RegistryService.AbstractSystemRegistryService;
import com.jd.live.agent.plugin.registry.dubbo.v2_6.instance.DubboEndpoint;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.alibaba.dubbo.common.Constants.CONSUMER_PROTOCOL;
import static com.jd.live.agent.core.util.CollectionUtils.toList;
import static com.jd.live.agent.plugin.registry.dubbo.v2_6.util.UrlUtils.*;


/**
 * Dubbo registry implementation that delegates to an underlying registry while providing additional functionality.
 * Handles service registration, discovery and notification with support for grouping and endpoint conversion.
 */
public class DubboRegistry extends AbstractSystemRegistryService implements Registry {

    private final Registry delegate;

    private final CompositeRegistry registry;

    private final Map<String, URL> subscribeUrls = new ConcurrentHashMap<>(16);

    private final Map<URL, Map<NotifyListener, DubboNotifyListener>> subscribes = new ConcurrentHashMap<>(16);

    private final Map<String, URL> registerUrls = new ConcurrentHashMap<>(16);

    private final AtomicBoolean registered = new AtomicBoolean(false);

    private final AtomicBoolean destroyed = new AtomicBoolean(false);

    public DubboRegistry(Registry delegate, CompositeRegistry registry) {
        super(getClusterName(delegate.getUrl()));
        this.delegate = delegate;
        this.registry = registry;
    }

    @Override
    public URL getUrl() {
        return delegate.getUrl();
    }

    @Override
    public boolean isAvailable() {
        return !isDestroy() && delegate.isAvailable();
    }

    @Override
    public void destroy() {
        if (destroyed.compareAndSet(false, true)) {
            registry.removeSystemRegistry(this);
            subscribes.forEach((url, listeners) -> listeners.forEach((k, v) -> v.close()));
            subscribes.clear();
            subscribeUrls.clear();
            registerUrls.clear();
            delegate.destroy();
        }
    }

    @Override
    public void register(URL url) {
        if (isDestroy()) {
            return;
        }
        // will call multiple times
        // Delay to ensure this registry is used.
        registerSystemRegistry();
        ServiceInstance instance = toInstance(url);
        registerUrls.put(instance.getUniqueName(), url);
        registry.register(instance, new RegistryRunnable(this, () -> {
            URL newUrl = registerUrls.get(instance.getUniqueName());
            if (newUrl != url) {
                // reregister
                return;
            }
            delegate.register(url);
        }));
    }

    @Override
    public void unregister(URL url) {
        ServiceInstance instance = toInstance(url);
        URL u = registerUrls.remove(instance.getUniqueName());
        if (u != null) {
            registry.unregister(instance);
            delegate.unregister(u);
        }
    }

    @Override
    public void unregister(ServiceId serviceId, ServiceInstance instance) {
        URL url = registerUrls.remove(serviceId.getUniqueName());
        if (url != null) {
            delegate.unregister(url);
        }
    }

    @Override
    public void subscribe(URL url, NotifyListener listener) {
        if (isDestroy()) {
            return;
        }
        // Delay to ensure this registry is used.
        registerSystemRegistry();
        if (CONSUMER_PROTOCOL.equals(url.getProtocol())) {
            DubboNotifyListener listen = createListener(url, listener);
            ServiceId serviceId = listen.getServiceId();
            subscribeUrls.put(serviceId.getUniqueName(), url);
            Map<NotifyListener, DubboNotifyListener> listeners = subscribes.computeIfAbsent(url, k -> new ConcurrentHashMap<>());
            if (listeners.putIfAbsent(listener, listen) == null) {
                // Start first to prevent notification loss during subscription
                listen.start();
                delegate.subscribe(listen.getUrl(), listen);
            }
        } else {
            delegate.subscribe(url, listener);
        }
    }

    @Override
    public void unsubscribe(URL url, NotifyListener listener) {
        if (CONSUMER_PROTOCOL.equals(url.getProtocol())) {
            Map<NotifyListener, DubboNotifyListener> listeners = subscribes.get(url);
            if (listeners != null) {
                DubboNotifyListener listen = listeners.remove(listener);
                if (listen != null) {
                    listen.close();
                    delegate.unsubscribe(url, listen);
                }
            }
            return;
        }
        delegate.unsubscribe(url, listener);
    }

    @Override
    public List<URL> lookup(URL url) {
        return isDestroy() ? new ArrayList<>(0) : delegate.lookup(url);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof DubboRegistry)) return false;
        DubboRegistry that = (DubboRegistry) o;
        return delegate == that.delegate;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(delegate);
    }

    @Override
    protected List<ServiceEndpoint> getEndpoints(ServiceId serviceId) {
        URL url = subscribeUrls.get(serviceId.getUniqueName());
        return url == null || isDestroy()
                ? new ArrayList<>()
                : toList(delegate.lookup(url), DubboEndpoint::new);
    }

    private DubboNotifyListener createListener(URL url, NotifyListener listener) {
        return new DubboNotifyListener(url, toServiceId(url), listener, this, null, registry);
    }

    private boolean isDestroy() {
        return destroyed.get();
    }

    private void registerSystemRegistry() {
        if (registered.compareAndSet(false, true)) {
            registry.addSystemRegistry(this);
        }
    }
}
