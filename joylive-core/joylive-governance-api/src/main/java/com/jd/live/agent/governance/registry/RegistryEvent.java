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
package com.jd.live.agent.governance.registry;

import lombok.Getter;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

@Getter
public class RegistryEvent {

    public static final AtomicLong VERSION = new AtomicLong(0);

    protected final long version;

    protected final ServiceId serviceId;

    protected final List<ServiceEndpoint> instances;

    protected final String defaultGroup;

    public RegistryEvent(String service, List<ServiceEndpoint> instances) {
        this(VERSION.incrementAndGet(), new ServiceId(service), instances, null);
    }

    public RegistryEvent(String service, String group, List<ServiceEndpoint> instances) {
        this(VERSION.incrementAndGet(), new ServiceId(service, group), instances, null);
    }

    public RegistryEvent(ServiceId serviceId, List<ServiceEndpoint> instances) {
        this(VERSION.incrementAndGet(), serviceId, instances, null);
    }

    public RegistryEvent(String service, String group, List<ServiceEndpoint> instances, String defaultGroup) {
        this(VERSION.incrementAndGet(), new ServiceId(service, group), instances, defaultGroup);
    }

    public RegistryEvent(ServiceId serviceId, List<ServiceEndpoint> instances, String defaultGroup) {
        this(VERSION.incrementAndGet(), serviceId, instances, defaultGroup);
    }

    public RegistryEvent(long version, String service, String group, List<ServiceEndpoint> instances, String defaultGroup) {
        this(version, new ServiceId(service, group), instances, defaultGroup);
    }

    public RegistryEvent(long version, ServiceId serviceId, List<ServiceEndpoint> instances, String defaultGroup) {
        this.version = version;
        this.serviceId = serviceId;
        this.instances = instances;
        this.defaultGroup = defaultGroup;
    }

    public boolean isEmpty() {
        return instances == null || instances.isEmpty();
    }

    public int size() {
        return instances == null ? 0 : instances.size();
    }

    public boolean isFull() {
        return true;
    }

}
