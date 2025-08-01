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
package com.jd.live.agent.plugin.registry.polaris.v2.registry;

import com.jd.live.agent.governance.registry.RegistryEvent;
import com.jd.live.agent.governance.registry.RegistryEventPublisher;
import com.jd.live.agent.governance.registry.ServiceEndpoint;
import com.jd.live.agent.plugin.registry.polaris.v2.instance.PolarisEndpoint;
import com.tencent.polaris.api.plugin.registry.ResourceEventListener;
import com.tencent.polaris.api.pojo.RegistryCacheValue;
import com.tencent.polaris.api.pojo.ServiceEventKey;
import com.tencent.polaris.api.pojo.ServiceEventKey.EventType;
import com.tencent.polaris.api.pojo.ServiceInstances;

import java.util.ArrayList;
import java.util.List;

import static com.jd.live.agent.core.util.CollectionUtils.toList;

public class PolarisEventListener implements ResourceEventListener {

    private final RegistryEventPublisher publisher;

    public PolarisEventListener(RegistryEventPublisher publisher) {
        this.publisher = publisher;
    }

    @Override
    public void onResourceAdd(ServiceEventKey svcEventKey, RegistryCacheValue newValue) {
        if (svcEventKey.getEventType() == EventType.INSTANCE) {
            ServiceInstances instances = (ServiceInstances) newValue;
            List<ServiceEndpoint> endpoints = toList(instances.getInstances(), PolarisEndpoint::new);
            publisher.publish(new RegistryEvent(instances.getService(), endpoints));
        }
    }

    @Override
    public void onResourceUpdated(ServiceEventKey svcEventKey, RegistryCacheValue oldValue, RegistryCacheValue newValue) {
        if (svcEventKey.getEventType() == EventType.INSTANCE) {
            ServiceInstances instances = (ServiceInstances) newValue;
            List<ServiceEndpoint> endpoints = toList(instances.getInstances(), PolarisEndpoint::new);
            publisher.publish(new RegistryEvent(instances.getService(), endpoints));
        }
    }

    @Override
    public void onResourceDeleted(ServiceEventKey svcEventKey, RegistryCacheValue oldValue) {
        if (svcEventKey.getEventType() == EventType.INSTANCE) {
            ServiceInstances instances = (ServiceInstances) oldValue;
            publisher.publish(new RegistryEvent(instances.getService(), new ArrayList<>()));
        }
    }
}
