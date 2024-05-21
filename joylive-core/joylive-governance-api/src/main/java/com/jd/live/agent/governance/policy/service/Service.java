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

import com.jd.live.agent.core.util.cache.Cache;
import com.jd.live.agent.core.util.cache.MapCache;
import com.jd.live.agent.core.util.map.ListBuilder;
import com.jd.live.agent.governance.policy.PolicyId;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Service
 *
 * @author Zhiguo.Chen
 * @since 1.0.0
 */
public class Service extends PolicyId {

    public static final String SERVICE_PREFIX = "service://";

    @Getter
    @Setter
    private String name;

    @Getter
    @Setter
    private ServiceType serviceType = ServiceType.HTTP;

    @Getter
    @Setter
    private Long version;

    @Getter
    @Setter
    private List<ServiceGroup> groups;

    @Getter
    private transient final Owner owners = new Owner();

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
    protected ServiceGroup getGroup(String group) {
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
        supplement(() -> SERVICE_PREFIX + name, supplementTag(KEY_SERVICE_NAME, name));
        if (groups != null) {
            for (ServiceGroup group : groups) {
                group.setServiceType(serviceType);
                group.supplement(() -> addPath(uri, group.getName()), supplementTag(KEY_SERVICE_GROUP, group.getName()));
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
     * Applies ownership to the service and its groups using the provided consumer.
     *
     * @param consumer The consumer to apply ownership logic.
     */
    public void own(Consumer<Owner> consumer) {
        if (consumer != null) {
            consumer.accept(owners);
        }
        if (groups != null) {
            for (ServiceGroup group : groups) {
                group.own(consumer);
            }
        }
    }

    /**
     * Recycles the service by removing groups that no longer have any owners.
     */
    protected void recycle() {
        if (groups != null) {
            List<ServiceGroup> targets = new ArrayList<>(groups.size());
            for (ServiceGroup group : groups) {
                if (!group.getOwners().isEmpty()) {
                    group.recycle();
                    targets.add(group);
                }
            }
            groups = targets;
        }
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
        result.tags = tags == null ? null : new HashMap<>(tags);
        result.name = name;
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

    /**
     * Merges the current service with another service instance, updating its groups and policies based on the provided service.
     * This method ensures that the ownership is updated and that the service groups and policies are merged according to the specified logic.
     * <p>
     * If the provided service is {@code null}, it triggers an ownership cleanup and recycles any groups without owners.
     * Otherwise, it updates or adds service groups from the provided service, applying the specified consumer to merge service policies.
     * This method is particularly useful for dynamically updating service configurations while maintaining proper ownership and custom policy behaviors.
     *
     * @param service  The service to merge with. If {@code null}, the method will clean up ownership and recycle groups accordingly.
     * @param consumer A {@link BiConsumer} that defines how to merge existing service policies with the new ones. This consumer
     *                 is applied to each matching service policy from the current and provided service groups.
     * @param owner    The owner identifier. This is used to update the ownership of the service and its groups during the merge process.
     *                 The owner is added to the current service's owners and to the owners of any updated or newly added groups.
     */
    public void merge(Service service, BiConsumer<ServicePolicy, ServicePolicy> consumer, String owner) {
        if (service == null) {
            own(o -> o.removeOwner(owner));
            recycle();
        } else {
            owners.addOwner(owner);
            if (groups == null || groups.isEmpty()) {
                groups = service.groups;
                defaultGroup = null;
                groupCache.clear();
            } else {
                List<ServiceGroup> targets = new ArrayList<>();
                for (ServiceGroup oldGroup : groups) {
                    Owner groupOwner = oldGroup.getOwners();
                    ServiceGroup newGroup = service.getGroup(oldGroup.getName());
                    if (newGroup == null) {
                        oldGroup.own(o -> o.removeOwner(owner));
                        oldGroup.recycle();
                        if (!groupOwner.isEmpty()) {
                            targets.add(oldGroup);
                        }
                    } else {
                        groupOwner.addOwner(owner);
                        oldGroup.merge(newGroup, consumer, owner);
                    }
                }
                for (ServiceGroup newGroup : service.groups) {
                    ServiceGroup oldGroup = getGroup(newGroup.getName());
                    if (oldGroup == null) {
                        targets.add(newGroup);
                    }
                }
                groups = targets;
                defaultGroup = null;
                groupCache.clear();
                service.groupCache.clear();
            }
        }
    }
}
