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

import com.jd.live.agent.bootstrap.util.type.FieldAccessor;
import com.jd.live.agent.core.instance.Application;
import com.jd.live.agent.core.parser.ObjectParser;
import com.jd.live.agent.core.util.Close;
import com.jd.live.agent.core.util.Locks;
import com.jd.live.agent.governance.registry.CompositeRegistry;
import com.jd.live.agent.governance.registry.RegistryRunnable;
import com.jd.live.agent.governance.registry.RegistryService.AbstractSystemRegistryService;
import com.jd.live.agent.governance.registry.ServiceEndpoint;
import com.jd.live.agent.governance.registry.ServiceId;
import com.jd.live.agent.plugin.registry.dubbo.v2_7.instance.DubboInstance;
import lombok.Getter;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.lang.Prioritized;
import org.apache.dubbo.common.utils.DefaultPage;
import org.apache.dubbo.common.utils.Page;
import org.apache.dubbo.registry.NotifyListener;
import org.apache.dubbo.registry.client.ServiceDiscovery;
import org.apache.dubbo.registry.client.ServiceInstance;
import org.apache.dubbo.registry.client.event.ServiceInstancesChangedEvent;
import org.apache.dubbo.registry.client.event.listener.ServiceInstancesChangedListener;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static com.jd.live.agent.bootstrap.util.type.FieldAccessorFactory.getAccessor;
import static com.jd.live.agent.core.util.CollectionUtils.toList;
import static com.jd.live.agent.plugin.registry.dubbo.v2_7.util.UrlUtils.*;
import static org.apache.dubbo.common.constants.CommonConstants.*;

public class DubboServiceDiscovery extends AbstractSystemRegistryService implements ServiceDiscovery {

    private static final Map<String, String> GROUPS = new HashMap<String, String>() {{
        put("org.apache.dubbo.registry.nacos.NacosServiceDiscovery", "DEFAULT_GROUP");
    }};

    private static final int UNREGISTER = 0;

    private static final int REGISTERED = 1;

    private static final int REGISTERED_SUCCESS = 2;

    private static final int DESTROYED = 3;

    private final URL url;

    private final ServiceDiscovery delegate;

    private final CompositeRegistry registry;

    private final Application application;

    private final ObjectParser parser;

    private final AtomicInteger status = new AtomicInteger(UNREGISTER);

    private final Map<ServiceInstancesChangedListener, DubboNotifyListener> listeners = new ConcurrentHashMap<>(16);

    @Getter
    private final String defaultGroup;

    private volatile ServiceInstance serviceInstance;

    public DubboServiceDiscovery(URL url, ServiceDiscovery delegate, CompositeRegistry registry, Application application, ObjectParser parser) {
        super(getClusterName(url));
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
        List<ServiceInstance> instances = isDestroy() ? new ArrayList<>() : delegate.getInstances(serviceId.getService());
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
        if (!isDestroy()) {
            status.set(DESTROYED);
            registry.removeSystemRegistry(this);
            delegate.destroy();
        }
    }

    @Override
    public void register(ServiceInstance serviceInstance) throws RuntimeException {
        // org.apache.dubbo.config.bootstrap.DubboBootstrap.registerServiceInstance() will invoke this method
        if (isDestroy()) {
            return;
        }
        if (status.compareAndSet(UNREGISTER, REGISTERED)) {
            this.serviceInstance = serviceInstance;
            registry.register(convert(serviceInstance), new RegistryRunnable(this, () -> Locks.read(lock, () -> {
                // Async invoke
                if (status.compareAndSet(REGISTERED, REGISTERED_SUCCESS)) {
                    // Retrieve new instances to avoid concurrent modification issues
                    delegate.register(this.serviceInstance);
                }
            })));
        }
    }

    @Override
    public void update(ServiceInstance serviceInstance) throws RuntimeException {
        Locks.write(lock, () -> {
            this.serviceInstance = serviceInstance;
            if (status.get() == REGISTERED_SUCCESS) {
                delegate.update(serviceInstance);
                // org.apache.dubbo.registry.nacos.NacosServiceDiscovery#update
                // super.update(serviceInstance);
                // unregister(serviceInstance);
                // register(serviceInstance);
            }
        });
    }

    @Override
    public void unregister(ServiceInstance serviceInstance) throws RuntimeException {
        // org.apache.dubbo.config.bootstrap.DubboBootstrap.unregisterServiceInstance() will invoke this method
        Locks.read(lock, () -> {
            com.jd.live.agent.governance.registry.ServiceInstance instance = convert(serviceInstance);
            if (status.compareAndSet(REGISTERED_SUCCESS, UNREGISTER)) {
                registry.unregister(instance);
                delegate.unregister(serviceInstance);
            }
        });
    }

    @Override
    public void unregister(ServiceId serviceId, com.jd.live.agent.governance.registry.ServiceInstance instance) {
        // called by live registry when shutdown
        Locks.read(lock, () -> {
            if (status.compareAndSet(REGISTERED_SUCCESS, UNREGISTER)) {
                delegate.unregister(serviceInstance);
            }
        });
    }

    @Override
    public Set<String> getServices() {
        return delegate.getServices();
    }

    @Override
    public ServiceInstance getLocalInstance() {
        return serviceInstance;
    }

    @Override
    public int getDefaultPageSize() {
        return delegate.getDefaultPageSize();
    }

    @Override
    public List<ServiceInstance> getInstances(String serviceName) throws NullPointerException {
        return isDestroy() ? new ArrayList<>() : delegate.getInstances(serviceName);
    }

    @Override
    public Page<ServiceInstance> getInstances(String serviceName, int offset, int pageSize) throws NullPointerException, IllegalArgumentException {
        return isDestroy() ? emptyPage() : delegate.getInstances(serviceName, offset, pageSize);
    }

    @Override
    public Page<ServiceInstance> getInstances(String serviceName, int offset, int pageSize, boolean healthyOnly) throws NullPointerException, IllegalArgumentException, UnsupportedOperationException {
        return isDestroy() ? emptyPage() : delegate.getInstances(serviceName, offset, pageSize, healthyOnly);
    }

    @Override
    public Map<String, Page<ServiceInstance>> getInstances(Iterable<String> serviceNames, int offset, int requestSize) throws NullPointerException, IllegalArgumentException {
        return isDestroy() ? new HashMap<>() : delegate.getInstances(serviceNames, offset, requestSize);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void addServiceInstancesChangedListener(ServiceInstancesChangedListener listener) throws NullPointerException, IllegalArgumentException {
        if (isDestroy()) {
            return;
        }
        URL url = listener.getUrl();
        ServiceId serviceId = toServiceId(url);
        String protocolServiceKey = url.getServiceKey() + GROUP_CHAR_SEPARATOR + url.getParameter(PROTOCOL_KEY, DUBBO);
        Map<String, Set<NotifyListener>> delegates = Accessor.listeners.get(listener, Map.class);
        listeners.computeIfAbsent(listener, l -> {
            Set<NotifyListener> notifiers = delegates.get(protocolServiceKey);
            NotifyListener old = notifiers.iterator().next();
            DubboNotifyListener wrapper = new DubboNotifyListener(url, serviceId, old, this, this, defaultGroup, registry);
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

    private boolean isDestroy() {
        return status.get() == DESTROYED;
    }

    private com.jd.live.agent.governance.registry.ServiceInstance convert(ServiceInstance serviceInstance) {
        return toInstance(serviceInstance, application, parser);
    }

    private DefaultPage<ServiceInstance> emptyPage() {
        return new DefaultPage<>(0, 10, new ArrayList<>(), 0);
    }

    private static class Accessor {

        private static final FieldAccessor listeners = getAccessor(ServiceInstancesChangedListener.class, "listeners");
    }
}
