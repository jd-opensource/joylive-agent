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
package com.jd.live.agent.plugin.registry.dubbo.v2_7.registry;

import com.jd.live.agent.core.instance.Application;
import com.jd.live.agent.core.parser.ObjectParser;
import com.jd.live.agent.core.util.Close;
import com.jd.live.agent.governance.registry.CompositeRegistry;
import com.jd.live.agent.governance.registry.RegistryRunnable;
import com.jd.live.agent.governance.registry.RegistryService.AbstractSystemRegistryService;
import com.jd.live.agent.governance.registry.ServiceEndpoint;
import com.jd.live.agent.governance.registry.ServiceId;
import com.jd.live.agent.plugin.registry.dubbo.v2_7.instance.DubboInstance;
import com.jd.live.agent.plugin.registry.dubbo.v2_7.util.UrlUtils;
import lombok.Getter;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.lang.Prioritized;
import org.apache.dubbo.common.utils.Page;
import org.apache.dubbo.registry.NotifyListener;
import org.apache.dubbo.registry.client.ServiceDiscovery;
import org.apache.dubbo.registry.client.ServiceInstance;
import org.apache.dubbo.registry.client.event.ServiceInstancesChangedEvent;
import org.apache.dubbo.registry.client.event.listener.ServiceInstancesChangedListener;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static com.jd.live.agent.bootstrap.util.type.UnsafeFieldAccessorFactory.getQuietly;
import static com.jd.live.agent.core.util.CollectionUtils.toList;
import static com.jd.live.agent.plugin.registry.dubbo.v2_7.util.UrlUtils.getSchemeAddress;
import static com.jd.live.agent.plugin.registry.dubbo.v2_7.util.UrlUtils.toInstance;
import static org.apache.dubbo.common.constants.CommonConstants.*;

public class DubboServiceDiscovery extends AbstractSystemRegistryService implements ServiceDiscovery {

    private static final Map<String, String> GROUPS = new HashMap<String, String>() {{
        put("org.apache.dubbo.registry.nacos.NacosServiceDiscovery", "DEFAULT_GROUP");
    }};

    private static final int UNREGISTER = 0;

    private static final int REGISTERED = 1;

    private static final int DESTROYED = 2;

    private final URL url;

    private final ServiceDiscovery delegate;

    private final CompositeRegistry registry;

    private final Application application;

    private final ObjectParser parser;

    private final AtomicInteger status = new AtomicInteger(UNREGISTER);

    private final Map<ServiceInstancesChangedListener, DubboNotifyListener> listeners = new ConcurrentHashMap<>(16);

    @Getter
    private final String defaultGroup;

    public DubboServiceDiscovery(URL url, ServiceDiscovery delegate, CompositeRegistry registry, Application application, ObjectParser parser) {
        super(getSchemeAddress(url));
        this.url = url;
        this.delegate = delegate;
        this.registry = registry;
        this.application = application;
        this.parser = parser;
        registry.addSystemRegistry(this);
        this.defaultGroup = GROUPS.get(delegate.getClass().getName());
    }

    @Override
    protected List<ServiceEndpoint> getEndpoints(ServiceId serviceId) throws Exception {
        List<ServiceInstance> instances = delegate.getInstances(serviceId.getService());
        String group = serviceId.getGroup();
        return toList(instances, instance -> {
            ServiceEndpoint endpoint = new DubboInstance(instance);
            String grp = endpoint.getGroup();
            if (grp == null || grp.isEmpty()) {
                return group == null || group.isEmpty() ? endpoint : null;
            }
            return grp.equals(group) ? endpoint : null;
        });
    }

    @Override
    public void initialize(URL registryURL) throws Exception {
        delegate.initialize(registryURL);
    }

    @Override
    public void destroy() throws Exception {
        status.set(DESTROYED);
        registry.removeSystemRegistry(this);
        delegate.destroy();
    }

    @Override
    public void register(ServiceInstance serviceInstance) throws RuntimeException {
        registry.register(toInstance(serviceInstance, application, parser),
                new RegistryRunnable(this, () -> {
                    if (status.get() == UNREGISTER) {
                        delegate.register(serviceInstance);
                        status.compareAndSet(UNREGISTER, REGISTERED);
                    }
                }));
    }

    @Override
    public void update(ServiceInstance serviceInstance) throws RuntimeException {
        if (status.get() == REGISTERED) {
            delegate.update(serviceInstance);
        }
    }

    @Override
    public void unregister(ServiceInstance serviceInstance) throws RuntimeException {
        registry.unregister(toInstance(serviceInstance, application, parser));
        delegate.unregister(serviceInstance);
    }

    @Override
    public Set<String> getServices() {
        return delegate.getServices();
    }

    @Override
    public ServiceInstance getLocalInstance() {
        return delegate.getLocalInstance();
    }

    @Override
    public int getDefaultPageSize() {
        return delegate.getDefaultPageSize();
    }

    @Override
    public List<ServiceInstance> getInstances(String serviceName) throws NullPointerException {
        return delegate.getInstances(serviceName);
    }

    @Override
    public Page<ServiceInstance> getInstances(String serviceName, int offset, int pageSize) throws NullPointerException, IllegalArgumentException {
        return delegate.getInstances(serviceName, offset, pageSize);
    }

    @Override
    public Page<ServiceInstance> getInstances(String serviceName, int offset, int pageSize, boolean healthyOnly) throws NullPointerException, IllegalArgumentException, UnsupportedOperationException {
        return delegate.getInstances(serviceName, offset, pageSize, healthyOnly);
    }

    @Override
    public Map<String, Page<ServiceInstance>> getInstances(Iterable<String> serviceNames, int offset, int requestSize) throws NullPointerException, IllegalArgumentException {
        return delegate.getInstances(serviceNames, offset, requestSize);
    }

    @Override
    public void addServiceInstancesChangedListener(ServiceInstancesChangedListener listener) throws NullPointerException, IllegalArgumentException {
        URL url = listener.getUrl();
        ServiceId serviceId = UrlUtils.toServiceId(url);
        String protocolServiceKey = url.getServiceKey() + GROUP_CHAR_SEPARATOR + url.getParameter(PROTOCOL_KEY, DUBBO);
        Map<String, Set<NotifyListener>> delegates = getQuietly(listener, "listeners");
        listeners.computeIfAbsent(listener, l -> {
            Set<NotifyListener> notifiers = delegates.get(protocolServiceKey);
            NotifyListener old = notifiers.iterator().next();
            DubboNotifyListener wrapper = new DubboNotifyListener(url, serviceId, old, this, defaultGroup, registry);
            notifiers.clear();
            notifiers.add(wrapper);
            delegate.addServiceInstancesChangedListener(listener);
            return wrapper;
        }).start();
    }

    @Override
    public void removeServiceInstancesChangedListener(ServiceInstancesChangedListener listener) throws IllegalArgumentException {
        Close.instance().close(listeners.remove(listener));
        delegate.removeServiceInstancesChangedListener(listener);
    }

    @Override
    public void dispatchServiceInstancesChangedEvent(String serviceName) {
        delegate.dispatchServiceInstancesChangedEvent(serviceName);
    }

    @Override
    public void dispatchServiceInstancesChangedEvent(String serviceName, String... otherServiceNames) {
        delegate.dispatchServiceInstancesChangedEvent(serviceName, otherServiceNames);
    }

    @Override
    public void dispatchServiceInstancesChangedEvent(String serviceName, List<ServiceInstance> serviceInstances) {
        delegate.dispatchServiceInstancesChangedEvent(serviceName, serviceInstances);
    }

    @Override
    public void dispatchServiceInstancesChangedEvent(ServiceInstancesChangedEvent event) {
        delegate.dispatchServiceInstancesChangedEvent(event);
    }

    @Override
    public URL getUrl() {
        return delegate.getUrl();
    }

    @Override
    public int getPriority() {
        return delegate.getPriority();
    }

    @Override
    public int compareTo(Prioritized that) {
        return delegate.compareTo(that);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof DubboServiceDiscovery)) return false;
        DubboServiceDiscovery that = (DubboServiceDiscovery) o;
        return delegate == that.delegate;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(delegate);
    }
}
