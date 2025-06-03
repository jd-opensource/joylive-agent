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
import com.alibaba.nacos.api.naming.listener.NamingEvent;
import com.alibaba.nacos.client.naming.NacosNamingService;
import com.jd.live.agent.core.util.Executors;
import com.jd.live.agent.core.util.URI;
import com.jd.live.agent.governance.config.RegistryClusterConfig;
import com.jd.live.agent.governance.registry.*;
import com.jd.live.agent.implement.service.registry.nacos.converter.InstanceConverter;
import com.jd.live.agent.implement.service.registry.nacos.converter.PropertiesConverter;

import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import static com.alibaba.nacos.api.PropertyKeyConst.SERVER_ADDR;
import static com.jd.live.agent.core.util.CollectionUtils.toList;
import static com.jd.live.agent.core.util.StringUtils.*;

/**
 * An implementation of the {@link Registry} interface specifically for Nacos.
 * This class provides functionality to register, unregister, and subscribe to services using Nacos.
 */
public class NacosRegistry implements RegistryService {

    protected final RegistryClusterConfig config;

    protected final String address;

    protected final String name;

    protected NamingService namingService;

    protected final AtomicBoolean started = new AtomicBoolean(false);

    public NacosRegistry(RegistryClusterConfig config) {
        this.config = config;
        this.address = join(toList(split(config.getAddress(), SEMICOLON_COMMA), URI::parse), uri -> uri.getAddress(true), CHAR_COMMA);
        this.name = "nacos://" + address;
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
        if (started.compareAndSet(false, true)) {
            Properties properties = PropertiesConverter.INSTANCE.convert(config);
            properties.put(SERVER_ADDR, address);
            namingService = Executors.call(this.getClass().getClassLoader(), () -> new NacosNamingService(properties));
        }
    }

    @Override
    public void close() {
        if (started.compareAndSet(true, false)) {
            if (namingService != null) {
                try {
                    namingService.shutDown();
                } catch (NacosException ignored) {
                }
            }
        }
    }

    @Override
    public void register(ServiceId serviceId, ServiceInstance instance) throws Exception {
        namingService.registerInstance(getService(serviceId, instance), getGroup(serviceId.getGroup()),
                InstanceConverter.INSTANCE.convert(instance));
    }

    @Override
    public void unregister(ServiceId serviceId, ServiceInstance instance) throws Exception {
        namingService.deregisterInstance(getService(serviceId, instance), getGroup(serviceId.getGroup()),
                InstanceConverter.INSTANCE.convert(instance));
    }

    @Override
    public void subscribe(ServiceId serviceId, Consumer<RegistryEvent> consumer) throws Exception {
        namingService.subscribe(serviceId.getService(), getGroup(serviceId.getGroup()), event -> {
            if (event instanceof NamingEvent) {
                NamingEvent e = (NamingEvent) event;
                ServiceId id = new ServiceId(e.getServiceName(), e.getGroupName(), serviceId.isInterfaceMode());
                consumer.accept(new RegistryEvent(id, toList(e.getInstances(), NacosEndpoint::new), Constants.DEFAULT_GROUP));
            }
        });
    }

    @Override
    public void unsubscribe(ServiceId serviceId) throws Exception {
        namingService.unsubscribe(serviceId.getService(), getGroup(serviceId.getGroup()), event -> {

        });
    }

    protected String getGroup(String group) {
        return group == null || group.isEmpty() ? Constants.DEFAULT_GROUP : group;
    }

    protected String getService(ServiceId serviceId, ServiceInstance instance) {
        return serviceId.getService();
    }
}
