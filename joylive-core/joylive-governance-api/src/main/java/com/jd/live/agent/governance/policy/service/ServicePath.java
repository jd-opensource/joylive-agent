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
import com.jd.live.agent.core.util.trie.Path;
import com.jd.live.agent.core.util.trie.PathMatchType;
import lombok.Getter;
import lombok.Setter;

import java.util.*;
import java.util.function.BiConsumer;

/**
 * Represents a service path within a service architecture.
 */
public class ServicePath extends ServicePolicyOwner implements Path {

    public static final String DEFAULT_NAME = "/";

    @Getter
    @Setter
    private String path;

    @Getter
    @Setter
    private PathMatchType matchType;

    @Getter
    @Setter
    private List<ServiceMethod> methods;

    private transient final Cache<String, ServiceMethod> methodCache = new MapCache<>(new ListBuilder<>(() -> methods, ServiceMethod::getName));

    private transient ServiceType serviceType;

    public ServicePath() {
    }

    public ServicePath(String path) {
        this.path = path;
    }

    /**
     * Retrieves a service method by its name from the cache.
     *
     * @param name The name of the service method to retrieve.
     * @return The ServiceMethod instance if found, otherwise {@code null}.
     */
    public ServiceMethod getMethod(String name) {
        return methodCache.get(name);
    }

    /**
     * Retrieves the service policy for a given method name.
     *
     * @param method The name of the method whose service policy is to be retrieved.
     * @return The ServicePolicy instance if the method exists, otherwise the service policy of this path.
     */
    public ServicePolicy getServicePolicy(String method) {
        ServiceMethod serviceMethod = getMethod(method);
        return serviceMethod == null ? servicePolicy : serviceMethod.getServicePolicy();
    }

    /**
     * Adds a service method to this path.
     *
     * @param method The ServiceMethod instance to be added to this path.
     */
    public void addMethod(ServiceMethod method) {
        if (methods == null) {
            methods = new ArrayList<>();
        }
        methods.add(method);
    }

    @Override
    protected void own(BiConsumer<ServicePolicy, Owner> consumer) {
        super.own(consumer);
        if (methods != null) {
            List<ServiceMethod> newMethods = new ArrayList<>(methods.size());
            for (ServiceMethod method : methods) {
                method.own(consumer);
                if (method.owners.hasOwner()) {
                    newMethods.add(method);
                }
            }
            if (newMethods.size() != methods.size()) {
                methods = newMethods;
                methodCache.clear();
            }
        }
    }

    protected ServicePath copy() {
        ServicePath result = new ServicePath();
        result.id = id;
        result.uri = uri;
        result.tags = tags == null ? null : new HashMap<>(tags);
        result.path = path;
        result.serviceType = serviceType;
        result.matchType = matchType;
        result.servicePolicy = servicePolicy == null ? null : servicePolicy.clone();
        result.methods = methods == null ? null : new ArrayList<>(methods.size());
        if (methods != null) {
            for (ServiceMethod path : methods) {
                result.methods.add(path.copy());
            }
        }
        result.owners.addOwner(owners);
        return result;
    }

    /**
     * Updates the current service path with the provided service path, using the specified policy merger and owner.
     * This method will handle the addition, update, or deletion of service methods as needed.
     *
     * @param path The service path containing the updates.
     * @param merger The policy merger to handle the merging logic.
     * @param owner The owner of the service path.
     */
    protected void onUpdate(ServicePath path, PolicyMerger merger, String owner) {
        onUpdate(path.servicePolicy, merger, owner);
        List<ServiceMethod> targets = new ArrayList<>();
        Set<String> olds = new HashSet<>();
        if (methods != null) {
            for (ServiceMethod old : methods) {
                olds.add(old.getName());
                ServiceMethod update = path.getMethod(old.getName());
                if (update == null) {
                    if (old.onDelete(merger, owner)) {
                        targets.add(old);
                    }
                } else {
                    old.onUpdate(update.getServicePolicy(), merger, owner);
                    targets.add(old);
                }
            }
        }
        if (path.methods != null) {
            for (ServiceMethod update : path.methods) {
                if (!olds.contains(update.getName())) {
                    update.onAdd(merger, owner);
                    targets.add(update);
                }
            }
        }
        methods = targets;
        methodCache.clear();
        path.methodCache.clear();
    }

    protected void setServiceType(ServiceType serviceType) {
        this.serviceType = serviceType;
    }

    /**
     * Supplements this path with a service policy, setting the match type based on the service type if not already set.
     *
     * @param source The source service policy used for supplementation.
     */
    protected void supplement(ServicePolicy source) {
        super.supplement(source);
        if (matchType == null) {
            switch (serviceType) {
                case HTTP:
                    matchType = PathMatchType.PREFIX;
                    break;
                case RPC_APP:
                case RPC_INTERFACE:
                default:
                    matchType = PathMatchType.EQUAL;
            }
        }
    }

    /**
     * Supplements the service methods of this path with those from a source ServicePath.
     *
     * @param source The source ServicePath used for supplementation.
     */
    protected void supplementMethod(ServicePath source) {
        if (source != null && source.methods != null && !source.methods.isEmpty()) {
            List<ServiceMethod> targets = new ArrayList<>(source.methods.size());
            ServiceMethod target;
            Set<Long> ids = new HashSet<>();
            for (ServiceMethod method : source.methods) {
                target = methodCache.get(method.getName());
                if (target != null) {
                    ids.add(supplementMethod(method, target).getId());
                } else {
                    targets.add(supplementMethod(method, new ServiceMethod(method.getName())));
                }
            }
            if (methods != null) {
                for (ServiceMethod method : methods) {
                    if (method.getId() == null || !ids.contains(method.getId())) {
                        supplementMethod(null, method);
                    }
                }
            }
            if (!targets.isEmpty()) {
                if (methods != null) {
                    targets.addAll(methods);
                }
                methods = targets;
                methodCache.clear();
            }
        } else if (methods != null) {
            for (ServiceMethod method : methods) {
                supplementMethod(null, method);
            }
        }
    }

    /**
     * Supplements a target service method with information from a source service method.
     *
     * @param source The source service method used for supplementation. Can be {@code null}.
     * @param target The target service method to be supplemented. Must not be {@code null}.
     * @return The supplemented target service method.
     */
    private ServiceMethod supplementMethod(ServiceMethod source, ServiceMethod target) {
        target.supplement(() -> addPath(uri, target.getName()),
                supplementTag(ServiceMethod.KEY_SERVICE_METHOD, target.getName()));
        target.supplement(source == null ? null : source.getServicePolicy());
        target.supplement(servicePolicy);
        return target;
    }

    /**
     * Caches this service path and its methods.
     */
    protected void cache() {
        super.cache();
        if (methods != null) {
            methods.forEach(ServiceMethod::cache);
        }
        methodCache.get("");
    }
}
