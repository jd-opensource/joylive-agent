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
package com.jd.live.agent.plugin.registry.nacos.v3_0.registry;

import com.alibaba.nacos.api.common.Constants;
import com.alibaba.nacos.api.naming.pojo.ServiceInfo;
import com.alibaba.nacos.client.naming.event.InstancesChangeEvent;
import com.alibaba.nacos.client.naming.event.InstancesChangeNotifier;
import com.alibaba.nacos.client.naming.selector.NamingSelectorWrapper;
import com.alibaba.nacos.common.notify.Event;
import com.jd.live.agent.governance.registry.RegistryEvent;
import com.jd.live.agent.governance.registry.ServiceEndpoint;
import lombok.Getter;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executor;

/**
 * Notifier for Nacos instance change events.
 * Extends {@link InstancesChangeNotifier} to handle Nacos-specific instance updates.
 */
public class NacosInstancesChangeNotifier extends InstancesChangeNotifier {

    private final InstancesChangeNotifier notifier;

    @Getter
    private final NacosRegistryPublisher publisher;

    public NacosInstancesChangeNotifier(String eventScope, InstancesChangeNotifier notifier, NacosRegistryPublisher publisher) {
        super(eventScope == null ? UUID.randomUUID().toString() : eventScope);
        this.notifier = notifier;
        this.publisher = publisher;
    }

    @Override
    public void registerListener(String groupName, String serviceName, NamingSelectorWrapper wrapper) {
        notifier.registerListener(groupName, serviceName, wrapper);
    }

    @Override
    public void deregisterListener(String groupName, String serviceName, NamingSelectorWrapper wrapper) {
        notifier.deregisterListener(groupName, serviceName, wrapper);
    }

    @Override
    public boolean isSubscribed(String groupName, String serviceName) {
        return notifier.isSubscribed(groupName, serviceName);
    }

    @Override
    public List<ServiceInfo> getSubscribeServices() {
        return notifier.getSubscribeServices();
    }

    @Override
    public void onEvent(InstancesChangeEvent event) {
        List<ServiceEndpoint> endpoints = publisher.convert(event.getHosts());
        publisher.publish(new RegistryEvent(event.getServiceName(), event.getGroupName(), endpoints, Constants.DEFAULT_GROUP));
        notifier.onEvent(event);
    }

    @Override
    public Class<? extends Event> subscribeType() {
        return notifier.subscribeType();
    }

    @Override
    public boolean scopeMatches(InstancesChangeEvent event) {
        return notifier.scopeMatches(event);
    }

    @Override
    public Executor executor() {
        return notifier.executor();
    }

    @Override
    public boolean ignoreExpireEvent() {
        return notifier.ignoreExpireEvent();
    }

}
