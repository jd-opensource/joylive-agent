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

import com.jd.live.agent.core.instance.Application;
import com.jd.live.agent.core.parser.ObjectParser;
import com.jd.live.agent.core.util.Close;
import com.jd.live.agent.governance.registry.CompositeRegistry;
import com.jd.live.agent.governance.registry.RegistryRunnable;
import com.jd.live.agent.governance.registry.RegistryService.AbstractSystemRegistryService;
import com.jd.live.agent.governance.registry.ServiceEndpoint;
import com.jd.live.agent.governance.registry.ServiceInstance;
import com.jd.live.agent.plugin.registry.dubbo.v3.instance.DubboInstance;
import lombok.Getter;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.lang.Prioritized;
import org.apache.dubbo.metadata.MetadataInfo;
import org.apache.dubbo.registry.NotifyListener;
import org.apache.dubbo.registry.client.DefaultServiceInstance;
import org.apache.dubbo.registry.client.ServiceDiscovery;
import org.apache.dubbo.registry.client.event.listener.ServiceInstancesChangedListener;
import org.apache.dubbo.registry.client.metadata.ServiceInstanceNotificationCustomizer;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.ScopeModelUtil;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static com.jd.live.agent.bootstrap.util.type.UnsafeFieldAccessorFactory.getQuietly;
import static com.jd.live.agent.bootstrap.util.type.UnsafeFieldAccessorFactory.setValue;
import static com.jd.live.agent.core.util.CollectionUtils.toList;
import static com.jd.live.agent.plugin.registry.dubbo.v3.util.UrlUtils.toInstance;
import static org.apache.dubbo.registry.Constants.CONSUMER_PROTOCOL;
import static org.apache.dubbo.registry.client.metadata.ServiceInstanceMetadataUtils.customizeInstance;
import static org.apache.dubbo.registry.client.metadata.ServiceInstanceMetadataUtils.setMetadataStorageType;

public class DubboServiceDiscovery extends AbstractSystemRegistryService implements ServiceDiscovery {

    private static final Map<String, String> GROUPS = new HashMap<String, String>() {{
        put("org.apache.dubbo.registry.nacos.NacosServiceDiscovery", "DEFAULT_GROUP");
    }};

    private static final int UNREGISTER = 0;

    private static final int REGISTERED = 1;

    private static final int REGISTERED_SUCCESS = 2;

    private final ServiceDiscovery delegate;

    private final CompositeRegistry registry;

    private final Application application;

    private final ObjectParser parser;

    private ServiceInstance instance;

    @Getter
    private final String defaultGroup;

    private final ApplicationModel model;

    private final AtomicInteger registered = new AtomicInteger(UNREGISTER);

    private final Map<URL, NotifyListener> subscribes = new ConcurrentHashMap<>(16);

    private final Map<ServiceInstancesChangedListener, List<DubboNotifyListener>> listeners = new ConcurrentHashMap<>(16);

    public DubboServiceDiscovery(ServiceDiscovery delegate, CompositeRegistry registry, Application application, ObjectParser parser) {
        this.delegate = delegate;
        this.registry = registry;
        this.application = application;
        this.parser = parser;
        this.defaultGroup = GROUPS.get(delegate.getClass().getName());
        this.model = ScopeModelUtil.getApplicationModel(delegate.getUrl().getScopeModel());
        registry.addSystemRegistry(this);
    }

    @Override
    protected List<ServiceEndpoint> getEndpoints(String service, String group) throws Exception {
        List<org.apache.dubbo.registry.client.ServiceInstance> instances = delegate.getInstances(service);
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
    public void destroy() throws Exception {
        registry.removeSystemRegistry(this);
        listeners.forEach((k, v) -> Close.instance().close(v));
        delegate.destroy();
    }

    @Override
    public void register() throws RuntimeException {
        doRegister();
    }

    @Override
    public void update() throws RuntimeException {
        if (registered.get() == REGISTERED_SUCCESS && !delegate.isDestroy()) {
            delegate.update();
        }
    }

    @Override
    public void unregister() throws RuntimeException {
        registry.unregister(instance);
        if (registered.get() == REGISTERED_SUCCESS) {
            delegate.unregister();
        }
    }

    @Override
    public void register(URL url) {
        // register url to metadata, then register instance to registry.
        delegate.register(url);
        doRegister();
    }

    @Override
    public void unregister(URL url) {
        // unregister from metadata
        delegate.unregister(url);
    }

    @Override
    public void subscribe(URL url, NotifyListener listener) {
        if (CONSUMER_PROTOCOL.equals(url.getProtocol())) {
            subscribes.putIfAbsent(url, listener);
        }
        delegate.subscribe(url, listener);
    }

    @Override
    public void unsubscribe(URL url, NotifyListener listener) {
        if (CONSUMER_PROTOCOL.equals(url.getProtocol())) {
            subscribes.remove(url);
        }
        delegate.unsubscribe(url, listener);
    }

    @Override
    public List<URL> lookup(URL url) {
        return delegate.lookup(url);
    }

    @Override
    public Set<String> getServices() {
        return delegate.getServices();
    }

    @Override
    public org.apache.dubbo.registry.client.ServiceInstance getLocalInstance() {
        return delegate.getLocalInstance();
    }

    @Override
    public List<org.apache.dubbo.registry.client.ServiceInstance> getInstances(String serviceName) throws NullPointerException {
        return delegate.getInstances(serviceName);
    }

    @Override
    public ServiceInstancesChangedListener createListener(Set<String> serviceNames) {
        // Dubbo will notify instances after creation
        ServiceInstancesChangedListener listener = new ServiceInstancesChangedListener(serviceNames, this);
        listeners.computeIfAbsent(listener, l -> {
            Set<ServiceInstanceNotificationCustomizer> customizers = getQuietly(l, "serviceInstanceNotificationCustomizers");
            List<DubboNotifyListener> listeners = new ArrayList<>(subscribes.size());
            subscribes.forEach((k, v) -> listeners.add(
                    new DubboNotifyListener(k, v, this, defaultGroup,
                            registry, model, customizers, this::getRemoteMetadata)));
            return listeners;
        });
        return listener;
    }

    @Override
    public void addServiceInstancesChangedListener(ServiceInstancesChangedListener listener) throws NullPointerException, IllegalArgumentException {
        delegate.addServiceInstancesChangedListener(listener);
        List<DubboNotifyListener> watchers = listeners.get(listener);
        if (watchers != null) {
            // Start watching live registry
            watchers.forEach(DubboNotifyListener::start);
        }
    }

    @Override
    public void removeServiceInstancesChangedListener(ServiceInstancesChangedListener listener) throws IllegalArgumentException {
        List<DubboNotifyListener> dubboListeners = listeners.remove(listener);
        Close.instance().close(dubboListeners);
        delegate.removeServiceInstancesChangedListener(listener);
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
    public boolean isDestroy() {
        return delegate.isDestroy();
    }

    @Override
    public MetadataInfo getLocalMetadata() {
        return delegate.getLocalMetadata();
    }

    @Override
    public MetadataInfo getRemoteMetadata(String revision) {
        return delegate.getRemoteMetadata(revision);
    }

    @Override
    public MetadataInfo getRemoteMetadata(String revision, List<org.apache.dubbo.registry.client.ServiceInstance> instances) {
        return delegate.getRemoteMetadata(revision, instances);
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

    private void doRegister() {
        // Call multiple times, only register once
        if (registered.get() != UNREGISTER || delegate.isDestroy()) {
            return;
        }
        // validate the host and port
        org.apache.dubbo.registry.client.ServiceInstance dubboInstance = build(delegate, application, parser);
        if (dubboInstance.getHost() == null || dubboInstance.getPort() <= 0) {
            return;
        }
        if (registered.compareAndSet(UNREGISTER, REGISTERED)) {
            instance = toInstance(dubboInstance, application, parser);
            registry.register(instance, new RegistryRunnable(this, () -> {
                if (delegate.isDestroy()) {
                    return;
                }
                // Delay register
                org.apache.dubbo.registry.client.ServiceInstance newInstance = build(delegate, application, parser);
                ServiceInstance current = toInstance(newInstance, application, parser);
                instance.setHost(current.getHost());
                instance.setPort(current.getPort());
                instance.setWeight(current.getWeight());
                instance.setMetadata(current.getMetadata());
                setValue(delegate, "serviceInstance", newInstance);
                delegate.register();
                registered.set(REGISTERED_SUCCESS);
            }));
        }

    }

    /**
     * Builds ServiceInstance from discovery service.
     *
     * @param discovery   the service discovery
     * @param application the application info
     * @param parser      the object parser
     * @return constructed service instance
     */
    private static org.apache.dubbo.registry.client.ServiceInstance build(ServiceDiscovery discovery, Application application, ObjectParser parser) {
        org.apache.dubbo.registry.client.ServiceInstance instance = discovery.getLocalInstance();
        if (instance == null) {
            MetadataInfo metadataInfo = discovery.getLocalMetadata();
            String serviceName = metadataInfo.getApp();
            ApplicationModel model = getQuietly(discovery, "applicationModel");
            String metadataType = getQuietly(discovery, "metadataType");
            instance = new DefaultServiceInstance(serviceName, model);
            instance.setServiceMetadata(metadataInfo);
            setMetadataStorageType(instance, metadataType);
            customizeInstance(instance, model);
        }
        Map<String, String> metadata = instance.getMetadata();
        if (metadata == null) {
            metadata = new HashMap<>();
            if (instance instanceof DefaultServiceInstance) {
                ((DefaultServiceInstance) instance).setMetadata(metadata);
            }
        }
        application.labelRegistry(metadata::putIfAbsent);
        return instance;
    }
}
