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

import lombok.Getter;
import org.apache.dubbo.registry.NotifyListener;
import org.apache.dubbo.registry.client.ServiceDiscovery;
import org.apache.dubbo.registry.client.event.listener.ServiceInstancesChangedListener;
import org.apache.dubbo.registry.client.metadata.ServiceInstanceNotificationCustomizer;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.ScopeModelUtil;

import java.util.Set;

@Getter
public class DubboServiceInstancesChangedListener extends ServiceInstancesChangedListener implements AutoCloseable {

    private final Set<ServiceInstanceNotificationCustomizer> customizers;

    private final ApplicationModel applicationModel;

    public DubboServiceInstancesChangedListener(Set<String> serviceNames, ServiceDiscovery serviceDiscovery) {
        super(serviceNames, serviceDiscovery);
        ApplicationModel applicationModel = ScopeModelUtil.getApplicationModel(
                serviceDiscovery == null || serviceDiscovery.getUrl() == null
                        ? null
                        : serviceDiscovery.getUrl().getScopeModel());
        this.customizers = applicationModel
                .getExtensionLoader(ServiceInstanceNotificationCustomizer.class)
                .getSupportedExtensionInstances();
        this.applicationModel = applicationModel;
    }

    public void addListener(DubboNotifyListener listener) {
        if (serviceNames.contains(listener.getServiceId().getService())) {
            listener.start();
            addListenerAndNotify(listener.getUrl(), listener);
        }
    }

    public void close() {
        listeners.forEach((k, v) -> v.forEach(notify -> {
            NotifyListener listener = notify.getNotifyListener();
            if (listener instanceof DubboNotifyListener) {
                ((DubboNotifyListener) listener).close();
            }
        }));
    }
}
