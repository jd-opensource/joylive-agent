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
import com.jd.live.agent.governance.registry.CompositeRegistry;
import com.jd.live.agent.governance.registry.RegistryRunnable;
import com.jd.live.agent.governance.registry.RegistryService.AbstractSystemRegistryService;
import com.jd.live.agent.governance.registry.ServiceEndpoint;
import com.jd.live.agent.governance.registry.ServiceId;
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

    private final Map<String, URL> urls = new ConcurrentHashMap<>(16);

    private final Map<URL, DubboNotifyListener> listeners = new ConcurrentHashMap<>(16);

    private final AtomicBoolean registered = new AtomicBoolean(false);

    public DubboRegistry(Registry delegate, CompositeRegistry registry) {
        super(getSchemeAddress(delegate.getUrl()));
        this.delegate = delegate;
        this.registry = registry;
    }

    @Override
    protected List<ServiceEndpoint> getEndpoints(ServiceId serviceId) throws Exception {
        URL url = urls.get(serviceId.getUniqueName());
        return url == null ? new ArrayList<>() : toList(delegate.lookup(url), DubboEndpoint::new);
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
        listeners.forEach((k, v) -> v.close());
        delegate.destroy();
    }

    @Override
    public void register(URL url) {
        // Delay to ensure this registry is used.
        addSystemRegistry();
        registry.register(toInstance(url), new RegistryRunnable(this, () -> delegate.register(url)));
    }

    @Override
    public void unregister(URL url) {
        registry.unregister(toInstance(url));
        delegate.unregister(url);
    }

    @Override
    public void subscribe(URL url, NotifyListener listener) {
        // Delay to ensure this registry is used.
        addSystemRegistry();
        if (CONSUMER_PROTOCOL.equals(url.getProtocol())) {
            ServiceId serviceId = toServiceId(url);
            String key = serviceId.getUniqueName();
            urls.put(key, url);
            DubboNotifyListener dubboListener = listeners.computeIfAbsent(url,
                    u -> new DubboNotifyListener(u, serviceId, listener, this, null, registry));
            dubboListener.start();
            delegate.subscribe(url, dubboListener);
        } else {
            delegate.subscribe(url, listener);
        }
    }

    @Override
    public void unsubscribe(URL url, NotifyListener listener) {
        if (CONSUMER_PROTOCOL.equals(url.getProtocol())) {
            DubboNotifyListener dubboListener = listeners.remove(url);
            if (dubboListener != null) {
                dubboListener.close();
                listener = dubboListener;
            }
        }
        delegate.unsubscribe(url, listener);
    }

    @Override
    public List<URL> lookup(URL url) {
        return delegate.lookup(url);
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

    private void addSystemRegistry() {
        if (registered.compareAndSet(false, true)) {
            registry.addSystemRegistry(this);
        }
    }
}
