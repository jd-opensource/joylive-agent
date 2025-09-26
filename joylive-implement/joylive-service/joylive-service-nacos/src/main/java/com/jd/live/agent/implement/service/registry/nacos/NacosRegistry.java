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
package com.jd.live.agent.implement.service.registry.nacos;

import com.alibaba.nacos.api.common.Constants;
import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.naming.NamingService;
import com.alibaba.nacos.api.naming.PreservedMetadataKeys;
import com.alibaba.nacos.api.naming.listener.Event;
import com.alibaba.nacos.api.naming.listener.EventListener;
import com.alibaba.nacos.api.naming.listener.NamingEvent;
import com.alibaba.nacos.api.naming.pojo.Instance;
import com.alibaba.nacos.client.naming.NacosNamingService;
import com.jd.live.agent.bootstrap.logger.Logger;
import com.jd.live.agent.bootstrap.logger.LoggerFactory;
import com.jd.live.agent.core.util.Locks;
import com.jd.live.agent.core.util.option.Option;
import com.jd.live.agent.core.util.task.RetryExecution;
import com.jd.live.agent.core.util.task.RetryVersionTimerTask;
import com.jd.live.agent.core.util.time.Timer;
import com.jd.live.agent.governance.config.RegistryClusterConfig;
import com.jd.live.agent.governance.policy.service.ServiceName;
import com.jd.live.agent.governance.probe.HealthProbe;
import com.jd.live.agent.governance.registry.*;
import com.jd.live.agent.implement.service.config.nacos.client.AbstractNacosClient;
import com.jd.live.agent.implement.service.registry.nacos.converter.InstanceConverter;
import com.jd.live.agent.implement.service.registry.nacos.converter.PropertiesConverter;
import lombok.Getter;

import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;

import static com.alibaba.nacos.ConnectionListener.LISTENER;
import static com.jd.live.agent.core.util.CollectionUtils.toList;
import static com.jd.live.agent.core.util.option.Converts.getInteger;

/**
 * An implementation of the {@link Registry} interface specifically for Nacos.
 * This class provides functionality to register, unregister, and subscribe to services using Nacos.
 */
public class NacosRegistry extends AbstractNacosClient<RegistryClusterConfig, NamingService> implements RegistryService {

    private static final Logger logger = LoggerFactory.getLogger(NacosRegistry.class);
    protected final String name;
    protected final Map<ServiceKey, Instance> registers = new ConcurrentHashMap<>();
    protected final Map<ServiceKey, InstanceListener> subscriptions = new ConcurrentHashMap<>();
    protected final ReadWriteLock registerLock = new ReentrantReadWriteLock();
    protected final ReadWriteLock subscriptionLock = new ReentrantReadWriteLock();

    public NacosRegistry(RegistryClusterConfig config, HealthProbe probe, Timer timer) {
        super(config, probe, timer);
        this.name = "nacos://" + server;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getDescription() {
        return name;
    }

    @Override
    public RegistryClusterConfig getConfig() {
        return config;
    }

    @Override
    public void start() throws Exception {
        doStart();
    }

    @Override
    public void close() {
        doClose();
    }

    @Override
    public void register(ServiceId serviceId, ServiceInstance instance) {
        String service = getService(serviceId, instance);
        String group = getGroup(serviceId.getGroup());
        Instance target = convert(instance);
        ServiceKey key = new ServiceKey(service, group);
        Locks.write(registerLock, () -> {
            if (registers.putIfAbsent(key, target) == null && connected.get()) {
                addTask(key, target, versions.get(), 0);
            }
        });
    }

    @Override
    public void unregister(ServiceId serviceId, ServiceInstance instance) throws Exception {
        String service = getService(serviceId, instance);
        String group = getGroup(serviceId.getGroup());
        ServiceKey key = new ServiceKey(service, group);
        Instance target = registers.remove(key);
        if (target != null) {
            client.deregisterInstance(service, group, target);
        }
    }

    @Override
    public void subscribe(ServiceId serviceId, Consumer<RegistryEvent> consumer) {
        String service = serviceId.getService();
        String group = getGroup(serviceId.getGroup());
        ServiceKey key = new ServiceKey(service, group);
        InstanceListener listener = new InstanceListener(serviceId.isInterfaceMode(), consumer);
        Locks.write(subscriptionLock, () -> {
            if (subscriptions.putIfAbsent(key, listener) == null && connected.get()) {
                addTask(key, listener, versions.get(), 0);
            }
        });
    }

    @Override
    public void unsubscribe(ServiceId serviceId) throws Exception {
        String service = serviceId.getService();
        String group = getGroup(serviceId.getGroup());
        ServiceKey key = new ServiceKey(service, group);
        if (subscriptions.remove(key) != null) {
            client.unsubscribe(service, group, event -> {

            });
        }
    }

    @Override
    protected int getInitializationTimeout(Option option) {
        return getInitializationTimeout(option, false);
    }

    @Override
    protected String getAddress(RegistryClusterConfig config) {
        return config.getAddress();
    }

    @Override
    protected Properties convert(RegistryClusterConfig config) {
        return PropertiesConverter.INSTANCE.convert(config);
    }

    @Override
    protected void recover() {
        // Avoid modification
        long version = versions.get();
        Locks.read(registerLock, () -> {
            for (Map.Entry<ServiceKey, Instance> entry : registers.entrySet()) {
                addTask(entry.getKey(), entry.getValue(), version, delay.get());
            }
        });
        Locks.read(subscriptionLock, () -> {
            for (Map.Entry<ServiceKey, InstanceListener> entry : subscriptions.entrySet()) {
                addTask(entry.getKey(), entry.getValue(), version, delay.get());
            }
        });
    }

    @Override
    protected void reconnect(String address) {
        // set listener thread local to add it in ServerListManager
        LISTENER.set(this::onDisconnected);
        try {
            doReconnect(address);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to connect to nacos " + address, e);
        } finally {
            LISTENER.remove();
        }
    }

    @Override
    protected NamingService createClient() throws NacosException {
        return new NacosNamingService(properties);
    }

    @Override
    protected void close(NamingService service) {
        if (service != null) {
            try {
                service.shutDown();
            } catch (NacosException ignored) {
            }
        }
    }

    /**
     * Returns group name or default if empty/null.
     *
     * @param group Input group name
     * @return Valid group name (never empty)
     */
    protected String getGroup(String group) {
        return group == null || group.isEmpty() ? Constants.DEFAULT_GROUP : group;
    }

    /**
     * Gets service name from service ID.
     *
     * @param serviceId Service identifier
     * @param instance  Service instance (unused)
     * @return Service name
     */
    protected String getService(ServiceId serviceId, ServiceInstance instance) {
        return serviceId.getService();
    }

    /**
     * Converts a ServiceInstance to Instance with optional metadata:
     * - heartbeat.interval (if valid and not already set)
     * - heartbeat.timeout (if valid and not already set)
     * - ip.delete.timeout (if valid and not already set)
     *
     * @param instance source service instance to convert
     * @return converted Instance with additional metadata (if configured)
     */
    protected Instance convert(ServiceInstance instance) {
        Instance target = InstanceConverter.INSTANCE.convert(instance);
        String heartbeatInterval = config.getProperty("heartbeat.interval");
        if (getInteger(heartbeatInterval, 0) > 0 && !target.containsMetadata(PreservedMetadataKeys.HEART_BEAT_INTERVAL)) {
            target.addMetadata(PreservedMetadataKeys.HEART_BEAT_INTERVAL, heartbeatInterval);
        }
        String heartbeatTimeout = config.getProperty("heartbeat.timeout");
        if (getInteger(heartbeatTimeout, 0) > 0 && !target.containsMetadata(PreservedMetadataKeys.HEART_BEAT_TIMEOUT)) {
            target.addMetadata(PreservedMetadataKeys.HEART_BEAT_TIMEOUT, heartbeatTimeout);
        }
        String ipDeleteTimeout = config.getProperty("ip.delete.timeout");
        if (getInteger(ipDeleteTimeout, 0) > 0 && !target.containsMetadata(PreservedMetadataKeys.IP_DELETE_TIMEOUT)) {
            target.addMetadata(PreservedMetadataKeys.IP_DELETE_TIMEOUT, ipDeleteTimeout);
        }
        return target;
    }

    /**
     * Schedules a versioned service subscription with initial delay.
     *
     * @param key      Service identifier to subscribe to
     * @param listener Callback for instance changes
     * @param version  Current subscription version
     * @param delay    Initial delay before first execution (ms)
     */
    protected void addTask(ServiceKey key, InstanceListener listener, long version, long delay) {
        SubscriptionTask subscription = new SubscriptionTask(key, listener);
        RetryVersionTimerTask task = new RetryVersionTimerTask("nacos.naming.subscription", subscription, version, predicate, timer);
        task.delay(delay);
    }

    /**
     * Schedules a version-aware service registration with delay.
     *
     * @param key      Service identifier
     * @param instance Instance to register
     * @param version  Current registration version
     * @param delay    Delay before execution (milliseconds)
     */
    protected void addTask(ServiceKey key, Instance instance, long version, long delay) {
        RegisterTask register = new RegisterTask(key, instance);
        RetryVersionTimerTask task = new RetryVersionTimerTask("nacos.naming.register", register, version, predicate, timer);
        task.delay(delay);
    }

    @Getter
    protected static class ServiceKey {

        protected final String service;

        protected final String group;

        ServiceKey(String service, String group) {
            this.service = service;
            this.group = group;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof ServiceKey)) return false;
            ServiceKey that = (ServiceKey) o;
            return Objects.equals(service, that.service) && Objects.equals(group, that.group);
        }

        @Override
        public int hashCode() {
            return Objects.hash(service, group);
        }
    }

    @Getter
    protected static class InstanceListener implements EventListener {

        private final boolean interfaceMode;

        private final Consumer<RegistryEvent> consumer;

        InstanceListener(boolean interfaceMode, Consumer<RegistryEvent> consumer) {
            this.interfaceMode = interfaceMode;
            this.consumer = consumer;
        }

        @Override
        public void onEvent(Event event) {
            if (event instanceof NamingEvent) {
                NamingEvent e = (NamingEvent) event;
                ServiceId id = new ServiceId(e.getServiceName(), e.getGroupName(), interfaceMode);
                consumer.accept(new RegistryEvent(id, toList(e.getInstances(), NacosEndpoint::new), Constants.DEFAULT_GROUP));
            }
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof InstanceListener)) return false;
            InstanceListener that = (InstanceListener) o;
            return Objects.equals(consumer, that.consumer);
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(consumer);
        }
    }

    /**
     * Retry task for failed Nacos config subscriptions.
     */
    private class SubscriptionTask implements RetryExecution {

        private final ServiceKey key;

        private final InstanceListener listener;

        SubscriptionTask(ServiceKey key, InstanceListener listener) {
            this.key = key;
            this.listener = listener;
        }

        @Override
        public Boolean call() throws Exception {
            if (subscriptions.get(key) == listener) {
                client.subscribe(key.getService(), key.group, listener);
                return true;
            } else {
                return true;
            }
        }

        @Override
        public long getRetryInterval() {
            return delay.get();
        }

    }

    /**
     * Retry task for failed Nacos config subscriptions.
     */
    private class RegisterTask implements RetryExecution {

        private final ServiceKey key;

        private final Instance instance;

        RegisterTask(ServiceKey key, Instance instance) {
            this.key = key;
            this.instance = instance;
        }

        @Override
        public Boolean call() throws Exception {
            if (registers.get(key) == instance) {
                client.registerInstance(key.getService(), key.group, instance);
                String uniqueName = ServiceName.getUniqueName(config.getNamespace(), key.getService(), key.getGroup());
                logger.info("Registered instance {}:{} to {} at {}", instance.getIp(), instance.getPort(), uniqueName, name);
            }
            return true;
        }

        @Override
        public long getRetryInterval() {
            return delay.get();
        }

    }
}
