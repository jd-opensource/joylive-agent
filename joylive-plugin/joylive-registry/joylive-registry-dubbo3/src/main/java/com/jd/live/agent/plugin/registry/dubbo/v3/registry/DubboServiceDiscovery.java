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

import com.jd.live.agent.bootstrap.util.type.FieldAccessor;
import com.jd.live.agent.core.instance.Application;
import com.jd.live.agent.core.parser.ObjectParser;
import com.jd.live.agent.core.util.Close;
import com.jd.live.agent.core.util.Locks;
import com.jd.live.agent.governance.registry.*;
import com.jd.live.agent.governance.registry.RegistryService.AbstractSystemRegistryService;
import com.jd.live.agent.plugin.registry.dubbo.v3.instance.DubboInstance;
import lombok.Getter;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.lang.Prioritized;
import org.apache.dubbo.metadata.MetadataInfo;
import org.apache.dubbo.registry.NotifyListener;
import org.apache.dubbo.registry.client.AbstractServiceDiscovery;
import org.apache.dubbo.registry.client.DefaultServiceInstance;
import org.apache.dubbo.registry.client.ServiceDiscovery;
import org.apache.dubbo.registry.client.event.listener.ServiceInstancesChangedListener;
import org.apache.dubbo.registry.client.metadata.ServiceInstanceNotificationCustomizer;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.ScopeModelUtil;

import java.util.*;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicInteger;

import static com.jd.live.agent.bootstrap.util.type.FieldAccessorFactory.getAccessor;
import static com.jd.live.agent.core.util.CollectionUtils.toList;
import static com.jd.live.agent.plugin.registry.dubbo.v3.util.UrlUtils.*;
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

    private static final int DESTROYED = 3;

    private final ServiceDiscovery delegate;

    private final CompositeRegistry registry;

    private final Application application;

    private final ObjectParser parser;

    @Getter
    private final String defaultGroup;

    private final ApplicationModel model;

    private final AtomicInteger status = new AtomicInteger(UNREGISTER);

    // synchronized in method
    private final Map<URL, Set<NotifyListener>> subscribes = new HashMap<>(16);

    // synchronized in method
    private final Set<DubboServiceInstancesChangedListener> listeners = new HashSet<>(16);

    private volatile ServiceInstance instance;

    public DubboServiceDiscovery(ServiceDiscovery delegate, CompositeRegistry registry, Application application, ObjectParser parser) {
        super(getClusterName(delegate.getUrl()));
        this.delegate = delegate;
        this.registry = registry;
        this.application = application;
        this.parser = parser;
        this.defaultGroup = GROUPS.get(delegate.getClass().getName());
        this.model = ScopeModelUtil.getApplicationModel(delegate.getUrl().getScopeModel());
        registry.addSystemRegistry(this);
    }

    @Override
    protected List<ServiceEndpoint> getEndpoints(ServiceId serviceId) throws Exception {
        List<org.apache.dubbo.registry.client.ServiceInstance> instances = delegate.getInstances(serviceId.getService());
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
    public void destroy() throws Exception {
        if (!isDestroy()) {
            status.set(DESTROYED);
            registry.removeSystemRegistry(this);
            Locks.read(lock, () -> {
                Close.instance().close(listeners);
            });
            delegate.destroy();
        }
    }

    @Override
    public void register() throws RuntimeException {
        doRegister();
    }

    @Override
    public void update() throws RuntimeException {
        if (status.get() == REGISTERED_SUCCESS) {
            delegate.update();
            // TODO check EXPORTED_SERVICES_REVISION_PROPERTY_NAME is changed and reregister.
        }
    }

    @Override
    public void unregister() throws RuntimeException {
        registry.unregister(instance);
        if (status.get() == REGISTERED_SUCCESS) {
            delegate.unregister();
        }
    }

    @Override
    public void register(URL url) {
        // register url to metadata, then register instance to registry.
        if (isDestroy()) {
            return;
        }
        delegate.register(url);
        doRegister();
    }

    @Override
    public void unregister(URL url) {
        // unregister from metadata
        delegate.unregister(url);
    }

    @Override
    public void unregister(ServiceId serviceId, ServiceInstance instance) {
        unregister();
    }

    @Override
    public void subscribe(URL url, NotifyListener listener) {
        if (CONSUMER_PROTOCOL.equals(url.getProtocol())) {
            ServiceId serviceId = toServiceId(url);
            Locks.write(lock, () -> {
                if (isDestroy()) {
                    return;
                }
                if (subscribes.computeIfAbsent(url, k -> new CopyOnWriteArraySet<>()).add(listener) && !listeners.isEmpty()) {
                    listeners.forEach(o ->
                            o.addListener(createListener(url, serviceId, listener, o.getCustomizers())));
                }
            });
        }
        delegate.subscribe(url, listener);
    }

    @Override
    public void unsubscribe(URL url, NotifyListener listener) {
        if (CONSUMER_PROTOCOL.equals(url.getProtocol())) {
            ServiceId serviceId = toServiceId(url);
            Locks.write(lock, () -> {
                if (isDestroy()) {
                    return;
                }
                Set<NotifyListener> notifiers = subscribes.get(url);
                if (notifiers != null && notifiers.remove(listener)) {
                    listeners.forEach(o ->
                            o.removeListener(url.getServiceKey(), createListener(url, serviceId, listener, o.getCustomizers())));
                }
            });
        }
        delegate.unsubscribe(url, listener);
    }

    @Override
    public List<URL> lookup(URL url) {
        return isDestroy() ? new ArrayList<>() : delegate.lookup(url);
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
        return isDestroy() ? new ArrayList<>() : delegate.getInstances(serviceName);
    }

    @Override
    public ServiceInstancesChangedListener createListener(Set<String> serviceNames) {
        // RegistryConstants.PROVIDED_BY supports multi-application mapping
        // Dubbo will notify instances after creation right now
        DubboServiceInstancesChangedListener result = new DubboServiceInstancesChangedListener(serviceNames, this);
        Locks.write(lock, () -> {
            if (!isDestroy()) {
                for (Map.Entry<URL, Set<NotifyListener>> entry : subscribes.entrySet()) {
                    for (NotifyListener listener : entry.getValue()) {
                        result.addListener(createListener(entry.getKey(), toServiceId(entry.getKey()), listener, result.getCustomizers()));
                    }
                }
                listeners.add(result);
            }
        });
        return result;
    }

    @Override
    public void addServiceInstancesChangedListener(ServiceInstancesChangedListener listener) throws NullPointerException, IllegalArgumentException {
        // call this method after createListener
        if (!isDestroy()) {
            delegate.addServiceInstancesChangedListener(listener);
        }
    }

    @Override
    public void removeServiceInstancesChangedListener(ServiceInstancesChangedListener listener) throws IllegalArgumentException {
        if (listener instanceof DubboServiceInstancesChangedListener) {
            DubboServiceInstancesChangedListener dubboListener = (DubboServiceInstancesChangedListener) listener;
            dubboListener.close();
            Locks.read(lock, () -> {
                if (listeners.remove(dubboListener)) {
                    delegate.removeServiceInstancesChangedListener(listener);
                }
            });
        } else {
            delegate.removeServiceInstancesChangedListener(listener);
        }
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
        return status.get() == DESTROYED;
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

    /**
     * Creates a new DubboNotifyListener with given parameters.
     *
     * @param url         the subscriber URL
     * @param serviceId   the target service ID
     * @param listener    the notification listener
     * @param customizers notification customizers
     * @return configured DubboNotifyListener instance
     */
    private DubboNotifyListener createListener(URL url,
                                               ServiceId serviceId,
                                               NotifyListener listener,
                                               Set<ServiceInstanceNotificationCustomizer> customizers) {
        return new DubboNotifyListener(url, serviceId, listener, this, defaultGroup, registry,
                model, customizers, this::getRemoteMetadata);
    }

    /**
     * Registers service instance if not already registered
     * Performs host/port validation and handles delayed registration
     */
    private void doRegister() {
        // Call multiple times, only register once
        if (isDestroy() || status.get() != UNREGISTER) {
            return;
        }
        // validate the host and port
        org.apache.dubbo.registry.client.ServiceInstance dubboInstance = build(delegate, application);
        if (dubboInstance == null || dubboInstance.getHost() == null || dubboInstance.getPort() <= 0) {
            return;
        }
        if (status.compareAndSet(UNREGISTER, REGISTERED)) {
            ServiceInstance instance = toInstance(dubboInstance, application, parser);
            this.instance = instance;
            registry.register(instance, new RegistryRunnable(this, () -> {
                if (delegate.isDestroy()) {
                    return;
                }
                // Delay register
                org.apache.dubbo.registry.client.ServiceInstance newInstance = build(delegate, application);
                if (newInstance != null) {
                    ServiceInstance current = toInstance(newInstance, application, parser);
                    ServiceInstance oldInstance = this.instance;
                    oldInstance.setHost(current.getHost());
                    oldInstance.setPort(current.getPort());
                    oldInstance.setWeight(current.getWeight());
                    oldInstance.setMetadata(current.getMetadata());
                    Accessor.serviceInstance.set(delegate, newInstance);
                }
                delegate.register();
                status.set(REGISTERED_SUCCESS);
            }));
        }

    }

    /**
     * Builds ServiceInstance from discovery service.
     *
     * @param discovery   the service discovery
     * @param application the application info
     * @return constructed service instance
     */
    private static org.apache.dubbo.registry.client.ServiceInstance build(ServiceDiscovery discovery, Application application) {
        org.apache.dubbo.registry.client.ServiceInstance instance = discovery.getLocalInstance();
        if (instance == null && discovery instanceof AbstractServiceDiscovery) {
            MetadataInfo metadataInfo = discovery.getLocalMetadata();
            String serviceName = metadataInfo.getApp();
            ApplicationModel model = ScopeModelUtil.getApplicationModel(
                    discovery.getUrl() == null ? null : discovery.getUrl().getScopeModel());
            String metadataType = Accessor.metadataType.get(discovery, String.class);
            instance = new DefaultServiceInstance(serviceName, model);
            instance.setServiceMetadata(metadataInfo);
            setMetadataStorageType(instance, metadataType);
            customizeInstance(instance, model);
        }
        if (instance != null) {
            Map<String, String> metadata = instance.getMetadata();
            if (metadata == null) {
                metadata = new HashMap<>();
                if (instance instanceof DefaultServiceInstance) {
                    ((DefaultServiceInstance) instance).setMetadata(metadata);
                }
            }
            application.labelRegistry(metadata::putIfAbsent);
        }
        return instance;
    }

    private static class Accessor {

        private static final FieldAccessor metadataType = getAccessor(AbstractServiceDiscovery.class, "metadataType");
        private static final FieldAccessor serviceInstance = getAccessor(AbstractServiceDiscovery.class, "serviceInstance");


    }
}
