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
import com.alibaba.nacos.api.naming.pojo.Instance;
import com.jd.live.agent.core.util.Executors;
import com.jd.live.agent.core.util.URI;
import com.jd.live.agent.governance.config.RegistryClusterConfig;
import com.jd.live.agent.governance.registry.Registry;
import com.jd.live.agent.governance.registry.RegistryEvent;
import com.jd.live.agent.governance.registry.RegistryService;
import com.jd.live.agent.governance.registry.ServiceInstance;
import com.alibaba.nacos.client.naming.NacosNamingService;

import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import static com.alibaba.nacos.api.PropertyKeyConst.*;
import static com.jd.live.agent.core.util.CollectionUtils.toList;
import static com.jd.live.agent.core.util.StringUtils.*;

/**
 * An implementation of the {@link Registry} interface specifically for Nacos.
 * This class provides functionality to register, unregister, and subscribe to services using Nacos.
 */
public class NacosRegistry implements RegistryService {

    private final RegistryClusterConfig config;

    private final String name;

    private NamingService namingService;

    private final AtomicBoolean started = new AtomicBoolean(false);

    public NacosRegistry(RegistryClusterConfig config) {
        this.config = config;
        URI uri = URI.parse(config.getAddress());
        this.name = "nacos://" + (uri == null ? "" : uri.getAddress());
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
            Properties properties = new Properties();
            List<URI> uris = toList(split(config.getAddress(), SEMICOLON_COMMA), URI::parse);
            String address = join(uris, uri -> uri.getAddress(true), CHAR_COMMA);
            properties.put(SERVER_ADDR, address);
            if (config.getConfig() != null) {
                properties.putAll(config.getConfig());
            }
            if (!isEmpty(config.getNamespace())) {
                properties.put(NAMESPACE, config.getNamespace());
            }
            if (!isEmpty(config.getUsername())) {
                properties.put(USERNAME, config.getUsername());
                properties.put(PASSWORD, config.getPassword());
            }
            if (config.isDenyEmptyEnabled()) {
                properties.put(NAMING_PUSH_EMPTY_PROTECTION, "true");
            }
            namingService = Executors.execute(this.getClass().getClassLoader(), () -> new NacosNamingService(properties));
        }
    }

    @Override
    public void close() {
        if (started.compareAndSet(true, false)) {
            try {
                namingService.shutDown();
            } catch (NacosException ignored) {
            }
        }
    }

    @Override
    public void register(String service, String group, ServiceInstance instance) throws Exception {
        namingService.registerInstance(service, getGroup(group), toInstance(instance));
    }

    @Override
    public void unregister(String service, String group, ServiceInstance instance) throws Exception {
        namingService.deregisterInstance(service, getGroup(group), toInstance(instance));
    }

    @Override
    public void subscribe(String service, String group, Consumer<RegistryEvent> consumer) throws Exception {
        namingService.subscribe(service, getGroup(group), event -> {
            if (event instanceof NamingEvent) {
                NamingEvent e = (NamingEvent) event;
                consumer.accept(new RegistryEvent(e.getServiceName(), e.getGroupName(), toList(e.getInstances(), NacosEndpoint::new), Constants.DEFAULT_GROUP));
            }
        });
    }

    @Override
    public void unsubscribe(String service, String group) throws Exception {
        namingService.unsubscribe(service, getGroup(group), event -> {

        });
    }

    private Instance toInstance(ServiceInstance instance) {
        Instance result = new Instance();
        result.setInstanceId(instance.getInstanceId());
        result.setIp(instance.getHost());
        result.setPort(instance.getPort());
        result.setServiceName(instance.getService());
        result.setMetadata(instance.getMetadata() == null ? null : new HashMap<>(instance.getMetadata()));
        result.setWeight(instance.getWeight());
        return result;
    }

    private String getGroup(String group) {
        return group == null || group.isEmpty() ? Constants.DEFAULT_GROUP : group;
    }
}
