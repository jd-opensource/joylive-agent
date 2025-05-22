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

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static com.alibaba.dubbo.common.Constants.CONSUMER_PROTOCOL;
import static com.jd.live.agent.core.util.CollectionUtils.toList;
import static com.jd.live.agent.plugin.registry.dubbo.v2_6.util.UrlUtils.parse;
import static com.jd.live.agent.plugin.registry.dubbo.v2_6.util.UrlUtils.toInstance;

public class DubboRegistry extends AbstractSystemRegistryService implements Registry {

    private static final Map<String, String> GROUPS = new HashMap<String, String>() {{
        put("org.apache.dubbo.registry.nacos.NacosRegistry", "DEFAULT_GROUP");
    }};

    private final Registry delegate;

    private final CompositeRegistry registry;

    private final Map<String, URL> urls = new ConcurrentHashMap<>(16);

    private final Map<NotifyListener, NotifyListener> listeners = new ConcurrentHashMap<>(16);

    public DubboRegistry(Registry delegate, CompositeRegistry registry) {
        this.delegate = delegate;
        this.registry = registry;
        registry.addSystemRegistry(this);
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
        delegate.destroy();
    }

    @Override
    public void register(URL url) {
        registry.register(toInstance(url), new RegistryRunnable(this, () -> delegate.register(url)));
    }

    @Override
    public void unregister(URL url) {
        registry.unregister(toInstance(url));
        delegate.unregister(url);
    }

    @Override
    public void subscribe(URL url, NotifyListener listener) {
        NotifyListener wrapper = listener;
        if (CONSUMER_PROTOCOL.equals(url.getProtocol())) {
            ServiceId serviceId = parse(url);
            String key = serviceId.getUniqueName();
            urls.put(key, url);
            wrapper = listeners.computeIfAbsent(listener,
                    l -> new DubboNotifyListener(url, l, this, GROUPS.get(delegate.getClass().getName())));
        }
        delegate.subscribe(url, wrapper);
    }

    @Override
    public void unsubscribe(URL url, NotifyListener listener) {
        NotifyListener wrapper = listener;
        if (CONSUMER_PROTOCOL.equals(url.getProtocol())) {
            wrapper = listeners.remove(listener);
            wrapper = wrapper == null ? listener : wrapper;
        }
        delegate.unsubscribe(url, wrapper);
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
}
