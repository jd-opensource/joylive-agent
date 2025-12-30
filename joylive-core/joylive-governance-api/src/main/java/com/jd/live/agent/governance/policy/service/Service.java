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

import com.jd.live.agent.core.Constants;
import com.jd.live.agent.core.util.CollectionUtils.Delta;
import com.jd.live.agent.core.util.URI;
import com.jd.live.agent.core.util.cache.Cache;
import com.jd.live.agent.core.util.cache.MapCache;
import com.jd.live.agent.core.util.map.ListBuilder;
import com.jd.live.agent.core.util.map.ListBuilder.LowercaseListBuilder;
import com.jd.live.agent.governance.policy.service.auth.AuthPolicy;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.Consumer;

import static com.jd.live.agent.core.util.CollectionUtils.diff;
import static com.jd.live.agent.core.util.CollectionUtils.toList;

/**
 * Service
 *
 * @author Zhiguo.Chen
 * @since 1.0.0
 */
public class Service extends PolicyOwner implements ServiceName {

    private static final URI SERVICE_URI = URI.builder().scheme("service").build();

    public static final BiPredicate<Service, Service> VERSION_PREDICATE = (o1, o2) -> o1.getVersion() != o2.getVersion();

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
    private Set<String> aliases;

    @Getter
    @Setter
    private long version = 1;

    @Getter
    @Setter
    private Boolean authorized;

    @Getter
    @Setter
    private AuthPolicy authPolicy;

    @Getter
    @Setter
    private List<AuthPolicy> authPolicies;

    @Getter
    @Setter
    private List<ServiceGroup> groups;

    @Getter
    private transient ServiceGroup defaultGroup;

    private transient final Cache<String, ServiceGroup> groupCache = new MapCache<>(new LowercaseListBuilder<>(() -> groups, null, ServiceGroup::getName));

    private transient final Cache<String, AuthPolicy> authPolicyCache = new MapCache<>(new ListBuilder<>(() -> authPolicies, AuthPolicy::getApplication));

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
                defaultGroup = new ServiceGroup(Constants.DEFAULT_GROUP, true, new ServicePolicy(), serviceType);
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

    public void alias(Consumer<String> consumer) {
        if (aliases != null) {
            aliases.forEach(consumer);
        }
    }

    public boolean authorized() {
        return authorized != null && authorized;
    }

    /**
     * Gets the authentication policy for specified application
     *
     * @param application Application identifier
     * @return AuthPolicy if found and matches, null otherwise
     */
    public AuthPolicy getAuthPolicy(String application) {
        // get consumer auth policy
        AuthPolicy result = authPolicyCache.get(application);
        return result == null && authPolicy != null && authPolicy.match(application) ? authPolicy : result;
    }

    /**
     * Supplements the service with default values and updates service groups accordingly.
     */
    protected void supplement() {
        if (serviceType == null) {
            serviceType = ServiceType.HTTP;
        }
        supplement(() -> SERVICE_URI.host(name));
        if (authPolicy != null) {
            authPolicy.supplement(() -> uri.parameter(KEY_SERVICE_AUTH, authPolicy.getApplication()));
        }
        if (authPolicies != null) {
            for (AuthPolicy authPolicy : authPolicies) {
                authPolicy.supplement(() -> uri.parameter(KEY_SERVICE_AUTH, authPolicy.getApplication()));
            }
        }
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
            // TODO Does not inherit default group policy
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
        getOrCreateDefaultGroup();
        supplement();
        if (groups != null) {
            groups.forEach(ServiceGroup::cache);
        }
        groupCache.get("");
        if (authPolicy != null) {
            authPolicy.cache();
        }
        if (authPolicies != null) {
            authPolicies.forEach(AuthPolicy::cache);
        }
        authPolicyCache.get("");
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
        result.aliases = aliases == null ? null : new HashSet<>(aliases);
        result.version = version;
        result.authorized = authorized;
        result.authPolicy = authPolicy == null ? null : authPolicy.copy();
        result.authPolicies = toList(authPolicies, AuthPolicy::copy);
        result.groups = toList(groups, ServiceGroup::copy);
        result.owners.addOwner(owners);
        return result;
    }

    @Override
    protected void own(BiConsumer<ServicePolicy, Owner> consumer) {
        if (consumer == null || groups == null) {
            return;
        }
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

    @Override
    protected void onAdd(PolicyMerger merger, String owner) {
        merger.onAdd(this);
        super.onAdd(merger, owner);
    }

    @Override
    protected boolean onDelete(PolicyMerger merger, String owner) {
        merger.onDelete(this);
        return super.onDelete(merger, owner);
    }

    /**
     * Updates service using specified merger and owner
     * @param newService Service containing new data
     * @param merger Policy merger to handle the update
     * @param owner Owner of the service
     */
    protected void onUpdate(Service newService, PolicyMerger merger, String owner) {
        merger.onUpdate(this, newService);
        owners.addOwner(owner);
        List<ServiceGroup> newGroups = new ArrayList<>();
        Delta<ServiceGroup> delta = diff(groups, newService.groups, ServiceGroup::getName);
        delta.getRemoves().forEach(v -> {
            if (v.onDelete(merger, owner)) {
                // has other owners
                newGroups.add(v);
            }
        });
        delta.getUpdates().forEach(v -> {
            v.getOldValue().onUpdate(v.getNewValue(), merger, owner);
            newGroups.add(v.getOldValue());
        });
        delta.getAdds().forEach(v -> {
            v.onAdd(merger, owner);
            newGroups.add(v);
        });
        version = newService.getVersion();
        groups = newGroups;
        defaultGroup = null;
        groupCache.clear();
        authPolicyCache.clear();
    }
}
