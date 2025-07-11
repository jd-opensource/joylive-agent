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

import com.jd.live.agent.governance.registry.*;
import com.jd.live.agent.governance.registry.RegistryService.AbstractSystemRegistryService;
import com.jd.live.agent.plugin.registry.dubbo.v3.instance.DubboEndpoint;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.registry.NotifyListener;
import org.apache.dubbo.registry.Registry;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.ScopeModelUtil;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

import static com.jd.live.agent.core.util.CollectionUtils.toList;
import static com.jd.live.agent.plugin.registry.dubbo.v3.util.UrlUtils.*;
import static org.apache.dubbo.registry.Constants.CONSUMER_PROTOCOL;

/**
 * Dubbo registry implementation that delegates to an underlying registry while providing additional functionality.
 * Handles service registration, discovery and notification with support for grouping and endpoint conversion.
 */
public class DubboRegistry extends AbstractSystemRegistryService implements Registry {

    private static final Map<String, String> GROUPS = new HashMap<String, String>() {{
        put("org.apache.dubbo.registry.nacos.NacosRegistry", "DEFAULT_GROUP");
    }};

    private final Registry delegate;

    private final CompositeRegistry registry;

    private final String defaultGroup;

    private final ApplicationModel model;

    private final Map<String, URL> subscribeUrls = new ConcurrentHashMap<>(16);

    private final Map<URL, Map<NotifyListener, DubboNotifyListener>> subscribes = new ConcurrentHashMap<>(16);

    private final Map<String, URLVersion> registerUrls = new ConcurrentHashMap<>(16);

    private final AtomicBoolean registered = new AtomicBoolean(false);

    private final AtomicBoolean destroyed = new AtomicBoolean(false);

    private final AtomicLong version = new AtomicLong(0);

    public DubboRegistry(Registry delegate, CompositeRegistry registry) {
        super(getClusterName(delegate.getUrl()));
        this.delegate = delegate;
        this.registry = registry;
        this.defaultGroup = GROUPS.get(delegate.getClass().getName());
        this.model = ScopeModelUtil.getApplicationModel(delegate.getUrl().getScopeModel());
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
        doRegister(url, delegate::register);
    }

    @Override
    public void unregister(URL url) {
        if (isDestroy()) {
            return;
        }
        doUnregister(url, delegate::unregister);
    }

    @Override
    public void unregister(ServiceId serviceId, ServiceInstance instance) {
        if (isDestroy()) {
            return;
        }
        URLVersion ver = registerUrls.remove(serviceId.getUniqueName());
        if (ver != null) {
            delegate.unregister(ver.getUrl());
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
        if (isDestroy()) {
            return;
        }
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
        return isDestroy() ? new ArrayList<>() : delegate.lookup(url);
    }

    @Override
    public void reExportRegister(URL url) {
        if (isDestroy()) {
            return;
        }
        // doReExport will call reExportUnregister -> reExportUnregister
        doRegister(url, delegate::reExportRegister);
    }

    @Override
    public void reExportUnregister(URL url) {
        if (isDestroy()) {
            return;
        }
        doUnregister(url, delegate::reExportUnregister);
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

    /**
     * Registers a service instance if not already registered.
     *
     * @param url      service URL to register
     * @param consumer callback to execute after successful registration
     */
    private void doRegister(URL url, Consumer<URL> consumer) {
        ServiceInstance instance = toInstance(url);
        String id = instance.getUniqueName();
        URLVersion ver = new URLVersion(url, version.incrementAndGet());
        if (registerUrls.putIfAbsent(id, ver) == null) {
            registry.register(instance, new RegistryRunnable(this, () -> {
                if (isDestroy()) {
                    return;
                }
                URLVersion newVer = registerUrls.get(id);
                if (newVer != ver) {
                    // reregister
                    return;
                }
                consumer.accept(url);
            }));
        }
    }

    /**
     * Unregisters a service instance and cleans up related resources.
     * @param url service URL to unregister
     * @param consumer callback to execute after successful unregistration
     */
    private void doUnregister(URL url, Consumer<URL> consumer) {
        ServiceInstance instance = toInstance(url, true);
        URLVersion ver = registerUrls.remove(instance.getUniqueName());
        if (ver != null) {
            // registry unregister will call unregister(ServiceId serviceId, ServiceInstance instance)
            // Remove from registerUrls first to prevent accidental calls
            registry.unregister(instance);
            consumer.accept(ver.getUrl());
        }
    }

    /**
     * Creates a notification listener for service discovery events.
     * @param url subscribed service URL
     * @param listener notification callback
     * @return configured DubboNotifyListener instance
     */
    private DubboNotifyListener createListener(URL url, NotifyListener listener) {
        return new DubboNotifyListener(url, toServiceId(url), listener, this, defaultGroup, registry, model, null, null);
    }

    /**
     * Checks if registry client is destroyed.
     * @return true if registry client is shutdown
     */
    private boolean isDestroy() {
        return destroyed.get();
    }

    /**
     * Registers this instance as system registry if not already registered.
     */
    private void registerSystemRegistry() {
        // Delay to ensure this registry is used.
        if (registered.compareAndSet(false, true)) {
            registry.addSystemRegistry(this);
        }
    }

    @Getter
    @AllArgsConstructor
    private static class URLVersion {

        private URL url;

        private long version;

    }
}
