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
package com.jd.live.agent.governance.policy.service;

import com.jd.live.agent.core.util.URI;
import com.jd.live.agent.core.util.cache.Cache;
import com.jd.live.agent.core.util.cache.MapCache;
import com.jd.live.agent.core.util.map.ListBuilder;
import com.jd.live.agent.governance.policy.PolicyId;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;

/**
 * Service
 *
 * @author Zhiguo.Chen
 * @since 1.0.0
 */
public class Service extends PolicyOwner implements ServiceName {

    private static final URI SERVICE_URI = URI.builder().schema("service").build();

    @Getter
    @Setter
    private String name;

    @Getter
    @Setter
    private String namespace;

    @Getter
    @Setter
    private ServiceType serviceType = ServiceType.HTTP;

    @Getter
    @Setter
    private long version;

    @Getter
    @Setter
    private List<ServiceGroup> groups;

    @Getter
    private transient ServiceGroup defaultGroup;

    private transient final Cache<String, ServiceGroup> groupCache = new MapCache<>(new ListBuilder<>(() -> groups, ServiceGroup::getName));

    public Service() {
    }

    /**
     * Constructs a service with the specified name.
     *
     * @param name The name of the service.
     */
    public Service(String name) {
        this(name, ServiceType.HTTP);
    }

    /**
     * Constructs a service with the specified name and service type.
     *
     * @param name        The name of the service.
     * @param serviceType The type of the service.
     */
    public Service(String name, ServiceType serviceType) {
        this.name = name;
        this.serviceType = serviceType;
    }

    /**
     * Retrieves or creates the default service group. If no default group exists, a new one is created and added.
     *
     * @return The default service group.
     */
    public ServiceGroup getOrCreateDefaultGroup() {
        if (defaultGroup == null) {
            if (groups != null) {
                for (ServiceGroup group : groups) {
                    if (group.isDefaultGroup()) {
                        defaultGroup = group;
                        break;
                    }
                }
            }
            if (defaultGroup == null) {
                defaultGroup = new ServiceGroup(PolicyId.DEFAULT_GROUP, true, new ServicePolicy(), serviceType);
                addGroup(defaultGroup);
            }
        }
        return defaultGroup;
    }

    /**
     * Adds a service group to the list of service groups.
     *
     * @param group The service group to add.
     */
    public void addGroup(ServiceGroup group) {
        if (group != null) {
            if (groups == null) {
                groups = new ArrayList<>();
            }
            groups.add(group);
        }
    }

    /**
     * Retrieves a service group by its name from the cache.
     *
     * @param group The name of the service group.
     * @return The service group, or {@code null} if not found.
     */
    public ServiceGroup getGroup(String group) {
        return groupCache.get(group);
    }

    /**
     * Retrieves a service policy based on the group, path, and method. If the group is not found, the default group is used.
     *
     * @param group  The name of the service group.
     * @param path   The service path.
     * @param method The service method.
     * @return The corresponding service policy, or {@code null} if not found.
     */
    public ServicePolicy getPath(String group, String path, String method) {
        ServiceGroup serviceGroup = getGroup(group);
        serviceGroup = serviceGroup == null ? defaultGroup : serviceGroup;
        return serviceGroup == null ? null : serviceGroup.getServicePolicy(path, method);
    }

    /**
     * Supplements the service with default values and updates service groups accordingly.
     */
    protected void supplement() {
        if (serviceType == null) {
            serviceType = ServiceType.HTTP;
        }
        supplement(() -> SERVICE_URI.host(name));
        if (groups != null) {
            for (ServiceGroup group : groups) {
                group.setServiceType(serviceType);
                group.supplement(() -> uri.parameter(KEY_SERVICE_GROUP, group.getName()));
                if (group.isDefaultGroup() && group != defaultGroup) {
                    defaultGroup = group;
                }
            }
            if (defaultGroup != null) {
                defaultGroup.supplement((ServiceGroup) null);
            }
            for (ServiceGroup group : groups) {
                if (group != defaultGroup) {
                    group.supplement(defaultGroup);
                }
            }
        }
    }

    /**
     * Caches the service and its groups, ensuring they are up to date.
     */
    public void cache() {
        supplement();
        if (groups != null) {
            groups.forEach(ServiceGroup::cache);
        }
        groupCache.get("");
    }

    /**
     * Creates a copy of this service.
     *
     * @return A new instance of {@code Service} that is a copy of this instance.
     */
    public Service copy() {
        Service result = new Service();
        result.id = id;
        result.uri = uri;
        result.name = name;
        result.namespace = namespace;
        result.serviceType = serviceType;
        result.version = version;
        result.groups = groups == null ? null : new ArrayList<>(groups.size());
        if (groups != null) {
            for (ServiceGroup group : groups) {
                result.groups.add(group.copy());
            }
        }
        result.owners.addOwner(owners);
        return result;
    }

    @Override
    protected void own(BiConsumer<ServicePolicy, Owner> consumer) {
        if (consumer != null && groups != null) {
            List<ServiceGroup> newGroups = new ArrayList<>(groups.size());
            for (ServiceGroup group : groups) {
                group.own(consumer);
                if (group.owners.hasOwner()) {
                    newGroups.add(group);
                }
            }
            if (newGroups.size() != groups.size()) {
                groups = newGroups;
                defaultGroup = null;
                groupCache.clear();
            }
        }
    }

    /**
     * Updates the current service with the provided service, using the specified policy merger and owner.
     * This method will handle the addition, update, or deletion of service groups as needed.
     *
     * @param service The service containing the updates.
     * @param merger The policy merger to handle the merging logic.
     * @param owner The owner of the service.
     */
    protected void onUpdate(Service service, PolicyMerger merger, String owner) {
        owners.addOwner(owner);
        List<ServiceGroup> targets = new ArrayList<>();
        Set<String> olds = new HashSet<>();
        if (groups != null) {
            for (ServiceGroup old : groups) {
                olds.add(old.getName());
                ServiceGroup update = service.getGroup(old.getName());
                if (update == null) {
                    if (old.onDelete(merger, owner)) {
                        targets.add(old);
                    }
                } else {
                    old.onUpdate(update, merger, owner);
                    targets.add(old);
                }
            }
        }
        if (service.groups != null) {
            for (ServiceGroup update : service.groups) {
                if (!olds.contains(update.getName())) {
                    update.onAdd(merger, owner);
                    targets.add(update);
                }
            }
        }
        groups = targets;
        defaultGroup = null;
        groupCache.clear();
        service.groupCache.clear();
    }
}
