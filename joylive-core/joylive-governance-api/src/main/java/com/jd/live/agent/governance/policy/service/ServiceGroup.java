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

import com.jd.live.agent.core.util.trie.PathMatchType;
import com.jd.live.agent.core.util.trie.PathMatcherTrie;
import com.jd.live.agent.core.util.trie.PathTrie;
import lombok.Getter;
import lombok.Setter;

import java.util.*;
import java.util.function.BiConsumer;

/**
 * Represents a logical grouping of service paths within a service, managing policies and ownership at the group level.
 * This class extends {@link ServicePolicyOwner} to incorporate policy ownership and management capabilities.
 */
public class ServiceGroup extends ServicePolicyOwner {

    @Setter
    @Getter
    private String name;

    @Getter
    @Setter
    private boolean defaultGroup;

    @Getter
    @Setter
    private List<ServicePath> paths;

    private transient ServiceType serviceType;

    private transient final PathTrie<ServicePath> pathCache = new PathMatcherTrie<>(() -> serviceType.getPathType().getDelimiter(), () -> paths);

    public ServiceGroup() {
    }

    /**
     * Constructs a service group with the specified name.
     *
     * @param name The name of the service group.
     */
    public ServiceGroup(String name) {
        this.name = name;
    }

    /**
     * Constructs a service group with the specified name, default group status, and service policy.
     *
     * @param name          The name of the service group.
     * @param defaultGroup  Indicates if this is the default group.
     * @param servicePolicy The service policy associated with this group.
     */
    public ServiceGroup(String name, boolean defaultGroup, ServicePolicy servicePolicy) {
        this.name = name;
        this.defaultGroup = defaultGroup;
        this.servicePolicy = servicePolicy;
    }

    /**
     * Constructs a service group with the specified name, default group status, service policy, and service type.
     *
     * @param name          The name of the service group.
     * @param defaultGroup  Indicates if this is the default group.
     * @param servicePolicy The service policy associated with this group.
     * @param serviceType   The type of service this group belongs to.
     */
    public ServiceGroup(String name, boolean defaultGroup, ServicePolicy servicePolicy, ServiceType serviceType) {
        this.name = name;
        this.defaultGroup = defaultGroup;
        this.servicePolicy = servicePolicy;
        this.serviceType = serviceType;
    }

    /**
     * Retrieves a service path by its path.
     *
     * @param path The path of the service path.
     * @return The service path, or {@code null} if not found.
     */
    public ServicePath getPath(String path) {
        return pathCache.get(path);
    }

    /**
     * Retrieves a service path by its path, using the specified path match type.
     *
     * @param path      The path of the service path.
     * @param matchType The type of path matching to use.
     * @return The service path, or {@code null} if not found.
     */
    public ServicePath match(String path, PathMatchType matchType) {
        path = serviceType.normalize(path);
        matchType = matchType == null ? serviceType.getMatchType() : matchType;
        return pathCache.match(path, matchType);
    }

    /**
     * Retrieves a service policy for a given path and method.
     *
     * @param path   The service path.
     * @param method The service method.
     * @return The corresponding service policy, or {@code null} if not found.
     */
    public ServicePolicy getServicePolicy(String path, String method) {
        return getServicePolicy(path, method, null);
    }

    /**
     * Retrieves a service policy for a given path and method, using the specified path match type.
     *
     * @param path      The service path.
     * @param method    The service method.
     * @param matchType The type of path matching to use.
     * @return The corresponding service policy, or {@code null} if not found.
     */
    public ServicePolicy getServicePolicy(String path, String method, PathMatchType matchType) {
        ServicePath servicePath = match(path, matchType);
        return servicePath == null ? servicePolicy : servicePath.getServicePolicy(method);
    }

    /**
     * Adds a service path to this group.
     *
     * @param path The service path to add.
     */
    public void addPath(ServicePath path) {
        if (path != null) {
            if (paths == null) {
                paths = new ArrayList<>();
            }
            paths.add(path);
        }
    }

    @Override
    protected void own(BiConsumer<ServicePolicy, Owner> consumer) {
        super.own(consumer);
        if (paths != null) {
            List<ServicePath> newPaths = new ArrayList<>(paths.size());
            for (ServicePath path : paths) {
                path.own(consumer);
                if (path.owners.hasOwner()) {
                    newPaths.add(path);
                }
            }
            if (newPaths.size() != paths.size()) {
                paths = newPaths;
                pathCache.clear();
            }
        }
    }

    protected ServiceGroup copy() {
        ServiceGroup result = new ServiceGroup();
        result.id = id;
        result.uri = uri;
        result.name = name;
        result.serviceType = serviceType;
        result.servicePolicy = servicePolicy == null ? null : servicePolicy.clone();
        result.defaultGroup = defaultGroup;
        result.paths = paths == null ? null : new ArrayList<>(paths.size());
        if (paths != null) {
            for (ServicePath path : paths) {
                result.paths.add(path.copy());
            }
        }
        result.owners.addOwner(owners);
        return result;
    }

    /**
     * Updates the current service group with the provided service group, using the specified policy merger and owner.
     * This method will handle the addition, update, or deletion of service paths as needed.
     *
     * @param group The service group containing the updates.
     * @param merger The policy merger to handle the merging logic.
     * @param owner The owner of the service group.
     */
    protected void onUpdate(ServiceGroup group, PolicyMerger merger, String owner) {
        onUpdate(group.servicePolicy, merger, owner);
        List<ServicePath> targets = new ArrayList<>();
        Set<String> olds = new HashSet<>();
        Map<String, ServicePath> newPaths = new HashMap<>();
        if (group.paths != null) {
            group.paths.forEach(path -> newPaths.put(path.getPath(), path));
        }
        if (paths != null) {
            for (ServicePath old : paths) {
                olds.add(old.getPath());
                ServicePath update = newPaths.get(old.getPath());
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
        if (group.paths != null) {
            for (ServicePath update : group.paths) {
                if (!olds.contains(update.getPath())) {
                    update.onAdd(merger, owner);
                    targets.add(update);
                }
            }
        }
        paths = targets;
        pathCache.clear();
        group.pathCache.clear();
    }

    protected void setServiceType(ServiceType serviceType) {
        this.serviceType = serviceType;
    }

    /**
     * Supplements this service group with data from another source group.
     *
     * @param source The source service group to supplement from.
     */
    protected void supplement(ServiceGroup source) {
        supplement(source == null ? null : source.getServicePolicy());
        supplementPath(source);
    }

    /**
     * Supplements the paths of this service group with the paths from the specified source service group.
     * This process involves updating existing paths with information from the source group and adding any new paths
     * that are present in the source but not in the current group. Paths in the current group that are not found
     * in the source are supplemented with default values or information specific to the current group.
     *
     * @param source The source service group from which to supplement paths. If {@code null}, the current paths are
     *               supplemented with default or specific information without reference to an external source.
     */
    private void supplementPath(ServiceGroup source) {
        if (source != null && source.paths != null && !source.paths.isEmpty()) {
            List<ServicePath> targets = new ArrayList<>(source.paths.size());
            ServicePath target;

            Set<Long> ids = new HashSet<>();
            Map<String, ServicePath> pathMap = new HashMap<>();
            if (paths != null) {
                paths.forEach(p -> pathMap.put(p.getPath(), p));
            }

            for (ServicePath path : source.paths) {
                target = pathMap.get(path.getPath());
                if (target != null) {
                    ids.add(supplementPath(path, target).getId());
                } else {
                    targets.add(supplementPath(path, new ServicePath(path.getPath())));
                }
            }

            if (paths != null) {
                for (ServicePath path : paths) {
                    if (path.getId() == null || !ids.contains(path.getId())) {
                        supplementPath(null, path);
                    }
                }
            }
            if (!targets.isEmpty()) {
                if (paths != null) {
                    targets.addAll(paths);
                }
                paths = targets;
                pathCache.clear();
            }
        } else if (paths != null) {
            for (ServicePath path : paths) {
                supplementPath(null, path);
            }
        }

    }

    /**
     * Supplements a target service path with information from a source service path. If the source path is {@code null},
     * the target is supplemented with default values or information specific to the current service group. This method
     * updates the target path's properties, including its path, service type, service policy, and method-specific information.
     *
     * @param source The source service path used for supplementation. Can be {@code null}, in which case the target is
     *               supplemented with default or current group-specific information.
     * @param target The target service path to be supplemented. Must not be {@code null}.
     * @return The supplemented target service path.
     */
    private ServicePath supplementPath(ServicePath source, ServicePath target) {
        String path = target.getPath() == null || target.getPath().isEmpty() ? ServicePath.DEFAULT_NAME : target.getPath();
        target.setPath(path);
        target.setServiceType(serviceType);
        target.supplement(() -> uri.path(path));
        target.supplement(source == null ? null : source.getServicePolicy());
        target.supplement(servicePolicy);
        target.supplementMethod(source);
        return target;
    }

    /**
     * Caches the service group and its paths, ensuring they are up to date.
     */
    protected void cache() {
        super.cache();
        if (paths != null) {
            paths.forEach(ServicePath::cache);
        }
        getPath("");
        match("", PathMatchType.EQUAL);
    }
}
