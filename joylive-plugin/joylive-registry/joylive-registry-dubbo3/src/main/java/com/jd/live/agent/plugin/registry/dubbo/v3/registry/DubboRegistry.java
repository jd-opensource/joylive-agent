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
import org.apache.dubbo.common.URL;
import org.apache.dubbo.registry.NotifyListener;
import org.apache.dubbo.registry.Registry;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.ScopeModelUtil;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

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

    private final Map<String, DubboSubscription> subscriptions = new ConcurrentHashMap<>(16);

    private final Map<String, URL> registerUrls = new ConcurrentHashMap<>(16);

    private final AtomicBoolean registered = new AtomicBoolean(false);

    public DubboRegistry(Registry delegate, CompositeRegistry registry) {
        super(getClusterName(delegate.getUrl()));
        this.delegate = delegate;
        this.registry = registry;
        this.defaultGroup = GROUPS.get(delegate.getClass().getName());
        this.model = ScopeModelUtil.getApplicationModel(delegate.getUrl().getScopeModel());
    }

    @Override
    protected List<ServiceEndpoint> getEndpoints(ServiceId serviceId) throws Exception {
        DubboSubscription subscription = subscriptions.get(serviceId.getUniqueName());
        return subscription == null ? new ArrayList<>() : toList(delegate.lookup(subscription.getUrl()), DubboEndpoint::new);
    }

    @Override
    public URL getUrl() {
        return delegate.getUrl();
    }

    @Override
    public boolean isAvailable() {
        return delegate.isAvailable();
    }

    @Override
    public void destroy() {
        registry.removeSystemRegistry(this);
        subscriptions.forEach((k, v) -> v.close());
        subscriptions.clear();
        delegate.destroy();
    }

    @Override
    public void register(URL url) {
        // Delay to ensure this registry is used.
        registerSystemRegistry();
        ServiceInstance instance = toInstance(url, true);
        registerUrls.put(instance.getUniqueName(), url);
        registry.register(instance, new RegistryRunnable(this, () -> delegate.register(url)));
    }

    @Override
    public void unregister(URL url) {
        ServiceInstance instance = toInstance(url, true);
        URL u = registerUrls.remove(instance.getUniqueName());
        if (u != null) {
            registry.unregister(instance);
            delegate.unregister(u);
        }
    }

    @Override
    public void unregister(ServiceId serviceId, ServiceInstance instance) throws Exception {
        URL u = registerUrls.remove(serviceId.getUniqueName());
        if (u != null) {
            delegate.unregister(u);
        }
    }

    @Override
    public void subscribe(URL url, NotifyListener listener) {
        // Delay to ensure this registry is used.
        registerSystemRegistry();
        if (CONSUMER_PROTOCOL.equals(url.getProtocol())) {
            ServiceId serviceId = toServiceId(url, true);
            DubboSubscription subscription = subscriptions.computeIfAbsent(serviceId.getUniqueName(),
                    k -> new DubboSubscription(k, serviceId, url));
            DubboNotifyListener listen = new DubboNotifyListener(subscription.getUrl(), serviceId, listener,
                    this, defaultGroup, registry, model, null, null);
            if (subscription.addListener(listen)) {
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
            AtomicReference<DubboNotifyListener> ref = new AtomicReference<>();
            subscriptions.computeIfPresent(toServiceId(url, true).getUniqueName(), (k, v) -> {
                ref.set(v.removeListener(listener));
                return v.isEmpty() ? null : v;
            });
            DubboNotifyListener listen = ref.get();
            if (listen != null) {
                listen.close();
                delegate.unsubscribe(listen.getUrl(), listen);
            }
        } else {
            delegate.unsubscribe(url, listener);
        }
    }

    @Override
    public List<URL> lookup(URL url) {
        return delegate.lookup(url);
    }

    @Override
    public void reExportRegister(URL url) {
        // reexport will call reExportUnregister -> reExportRegister
        ServiceInstance instance = toInstance(url, true);
        registerUrls.put(instance.getUniqueName(), url);
        delegate.reExportRegister(url);
    }

    @Override
    public void reExportUnregister(URL url) {
        // reexport will call reExportUnregister -> reExportRegister
        URL u = registerUrls.remove(toServiceId(url, true).getUniqueName());
        if (u != null) {
            delegate.reExportUnregister(u);
        }
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

    private void registerSystemRegistry() {
        // Delay to ensure this registry is used.
        if (registered.compareAndSet(false, true)) {
            registry.addSystemRegistry(this);
        }
    }
}
