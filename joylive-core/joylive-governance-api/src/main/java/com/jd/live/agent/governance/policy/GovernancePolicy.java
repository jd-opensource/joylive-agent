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
package com.jd.live.agent.governance.policy;

import com.jd.live.agent.core.instance.Location;
import com.jd.live.agent.core.util.URI;
import com.jd.live.agent.core.util.cache.Cache;
import com.jd.live.agent.core.util.cache.MapCache;
import com.jd.live.agent.core.util.cache.UnsafeLazyObject;
import com.jd.live.agent.core.util.map.ListBuilder;
import com.jd.live.agent.governance.policy.db.DatabaseCluster;
import com.jd.live.agent.governance.policy.domain.Domain;
import com.jd.live.agent.governance.policy.domain.DomainPolicy;
import com.jd.live.agent.governance.policy.lane.LaneDomain;
import com.jd.live.agent.governance.policy.lane.LaneSpace;
import com.jd.live.agent.governance.policy.live.LiveDomain;
import com.jd.live.agent.governance.policy.live.LiveSpace;
import com.jd.live.agent.governance.policy.live.LiveSpec;
import com.jd.live.agent.governance.policy.live.UnitDomain;
import com.jd.live.agent.governance.policy.service.*;
import lombok.Getter;
import lombok.Setter;

import java.util.*;

/**
 * Represents the governance policy for managing resources and configurations.
 * <p>
 * This class encapsulates the governance policy, including live spaces, lane spaces, services, and database clusters.
 * It provides caching mechanisms for efficient retrieval of domain, service, and database cluster information.
 * </p>
 */
public class GovernancePolicy {

    @Setter
    @Getter
    private List<LiveSpace> liveSpaces;

    @Getter
    @Setter
    private List<LaneSpace> laneSpaces;

    @Setter
    @Getter
    private List<Service> services;

    @Setter
    @Getter
    private List<DatabaseCluster> dbClusters;

    @Getter
    private transient LiveSpace localLiveSpace;

    @Getter
    private transient LaneSpace localLaneSpace;

    private final transient UnsafeLazyObject<LaneSpace> defaultLaneSpaceCache = new UnsafeLazyObject<>(() -> {
        if (laneSpaces != null) {
            for (LaneSpace laneSpace : laneSpaces) {
                if (laneSpace.isDefaultSpace()) {
                    return laneSpace;
                }
            }
        }
        return null;
    });

    private final transient Cache<String, DatabaseCluster> dbAddressCache = new MapCache<>(new ListBuilder<>(() -> dbClusters, DatabaseCluster::getAddress));

    private final transient Cache<String, DatabaseCluster> dbNameCache = new MapCache<>(new ListBuilder<>(() -> dbClusters, DatabaseCluster::getName));

    private final transient Cache<String, LiveSpace> liveSpaceCache = new MapCache<>(new ListBuilder<>(() -> liveSpaces, LiveSpace::getId));

    private final transient Cache<String, LaneSpace> laneSpaceCache = new MapCache<>(new ListBuilder<>(() -> laneSpaces, LaneSpace::getId));

    private final transient Cache<String, Domain> domainCache = new MapCache<>(() -> {
        Map<String, Domain> laneDomains = new HashMap<>();
        if (laneSpaces != null) {
            for (LaneSpace laneSpace : laneSpaces) {
                if (laneSpace.getDomains() != null) {
                    for (LaneDomain laneDomain : laneSpace.getDomains()) {
                        laneDomains.put(laneDomain.getHost(), new Domain(laneDomain.getHost(), new DomainPolicy(laneSpace, laneDomain)));
                    }
                }
            }
        }

        Map<String, Domain> liveDomains = new HashMap<>();
        if (liveSpaces != null) {
            for (LiveSpace liveSpace : liveSpaces) {
                LiveSpec liveSpec = liveSpace.getSpec();
                if (liveSpec.getDomains() != null) {
                    for (LiveDomain liveDomain : liveSpec.getDomains()) {
                        liveDomains.put(liveDomain.getHost(), new Domain(liveDomain.getHost(), new DomainPolicy(liveSpace, liveDomain)));
                        if (liveDomain.getUnitDomains() != null) {
                            for (UnitDomain unitDomain : liveDomain.getUnitDomains()) {
                                liveDomains.put(unitDomain.getHost(), new Domain(unitDomain.getHost(), new DomainPolicy(liveSpace, liveDomain, unitDomain)));
                            }
                        }
                    }
                }
            }
        }

        Map<String, Domain> result = new HashMap<>(Integer.max(liveDomains.size(), laneDomains.size()));
        for (Domain liveDomain : liveDomains.values()) {
            Domain laneDomain = laneDomains.get(liveDomain.getHost());
            DomainPolicy lanePolicy = laneDomain == null ? null : laneDomain.getPolicy();
            DomainPolicy livePolicy = liveDomain.getPolicy();
            result.put(liveDomain.getHost(), laneDomain == null ? liveDomain : new Domain(liveDomain.getHost(),
                    new DomainPolicy(livePolicy.getLiveSpace(), livePolicy.getLiveDomain(), livePolicy.getUnitDomain(),
                            lanePolicy.getLaneSpace(), lanePolicy.getLaneDomain())));
        }
        for (Domain laneDomain : laneDomains.values()) {
            result.putIfAbsent(laneDomain.getHost(), laneDomain);
        }
        return result;
    });

    private final transient Cache<String, Service> serviceCache = new MapCache<>(new ListBuilder<>(() -> services, Service::getName));

    /**
     * Default constructor for GovernancePolicy.
     */
    public GovernancePolicy() {
    }

    /**
     * Constructs a new GovernancePolicy with specified live spaces and services.
     *
     * @param liveSpaces The list of live spaces governed by this policy.
     * @param services   The list of services governed by this policy.
     */
    public GovernancePolicy(List<LiveSpace> liveSpaces, List<Service> services) {
        this.liveSpaces = liveSpaces;
        this.services = services;
    }

    /**
     * Retrieves a {@link LiveSpace} by its ID.
     *
     * @param id The ID of the live space to retrieve.
     * @return The live space with the specified ID, or {@code null} if not found.
     */
    public LiveSpace getLiveSpace(String id) {
        return id == null ? null : liveSpaceCache.get(id);
    }

    /**
     * Retrieves a {@link LaneSpace} by its ID.
     *
     * @param id The ID of the lane space to retrieve.
     * @return The lane space with the specified ID, or {@code null} if not found.
     */
    public LaneSpace getLaneSpace(String id) {
        return id == null || id.isEmpty() ? defaultLaneSpaceCache.get() : laneSpaceCache.get(id);
    }

    public LaneSpace getDefaultLaneSpace() {
        return defaultLaneSpaceCache.get();
    }

    /**
     * Retrieves a {@link Domain} by its host name.
     *
     * @param host The host name of the domain to retrieve.
     * @return The domain with the specified host name, or {@code null} if not found.
     */
    public Domain getDomain(String host) {
        return domainCache.get(host);
    }

    /**
     * Retrieves a {@link Service} by its name.
     *
     * @param name The name of the service to retrieve.
     * @return The service with the specified name, or {@code null} if not found.
     */
    public Service getService(String name) {
        return serviceCache.get(name);
    }

    /**
     * Retrieves the {@link ServicePolicy} for a given URI.
     *
     * @param uri The {@link URI} from which to extract the service information.
     * @return The {@link ServicePolicy} associated with the URI, or {@code null} if no policy is found.
     */
    public ServicePolicy getServicePolicy(URI uri) {
        if (uri == null) {
            return null;
        }
        String serviceName = uri.getHost();
        String servicePath = uri.getPath();
        String serviceGroup = uri.getParameter(PolicyId.KEY_SERVICE_GROUP);
        String serviceMethod = uri.getParameter(PolicyId.KEY_SERVICE_METHOD);

        Service service = getService(serviceName);
        ServiceGroup group = service == null ? null : service.getGroup(serviceGroup);
        ServicePath path = group == null ? null : group.getPath(servicePath);
        ServiceMethod method = path == null ? null : path.getMethod(serviceMethod);

        ServicePolicy servicePolicy = method != null ? method.getServicePolicy() : null;
        servicePolicy = servicePolicy == null && path != null ? path.getServicePolicy() : servicePolicy;
        servicePolicy = servicePolicy == null && group != null ? group.getServicePolicy() : servicePolicy;
        return servicePolicy;
    }

    /**
     * Retrieves a {@link DatabaseCluster} by its name.
     *
     * @param name The name of the database cluster to retrieve.
     * @return The database cluster with the specified name, or {@code null} if not found.
     */
    public DatabaseCluster getDbCluster(String name) {
        return dbNameCache.get(name);
    }

    /**
     * Retrieves a {@link DatabaseCluster} by its host and port.
     *
     * @param host The host of the database cluster to retrieve.
     * @param port The port of the database cluster.
     * @return The database cluster with the specified host and port, or {@code null} if not found.
     */
    public DatabaseCluster getDbCluster(String host, int port) {
        return dbAddressCache.get(port <= 0 ? host : host + ":" + port);
    }

    /**
     * Locates the given location in the live space and lane space.
     *
     * @param location the location to locate
     */
    public void locate(Location location) {
        if (location == null) {
            return;
        }
        localLiveSpace = getLiveSpace(location.getLiveSpaceId());
        if (localLiveSpace != null) {
            localLiveSpace.locate(location.getUnit(), location.getCell());
        }
        localLaneSpace = getLaneSpace(location.getLaneSpaceId());
        if (localLaneSpace != null) {
            localLaneSpace.locate(location.getLane());
        }
    }

    /**
     * Populates the caches with initial values.
     * <p>
     * This method should be called to populate the caches after initialization or when the underlying data changes.
     * </p>
     */
    public void cache() {
        getLiveSpace("");
        getLaneSpace("");
        getDomain("");
        getService("");
        getDbCluster("");
        getDbCluster("", 0);
        getDefaultLaneSpace();

        if (liveSpaces != null) {
            liveSpaces.forEach(LiveSpace::cache);
        }
        if (laneSpaces != null) {
            laneSpaces.forEach(LaneSpace::cache);
        }
        if (services != null) {
            services.forEach(Service::cache);
        }
        if (dbClusters != null) {
            dbClusters.forEach(DatabaseCluster::cache);
        }
    }

    /**
     * Updates services, using the specified policy merger and owner.
     *
     * @param updates The list of services to update the current services with.
     * @param deletes The list of services to be deleted.
     * @param merger The policy merger to handle the merging logic.
     * @param owner The owner of the services.
     * @return The updated list of services.
     */
    public List<Service> onUpdate(List<Service> updates, Set<String> deletes, PolicyMerger merger, String owner) {
        List<Service> result = new ArrayList<>();
        Map<String, Service> updateMap = new HashMap<>(updates.size());
        updates.forEach(o -> updateMap.put(o.getName(), o));
        Set<String> olds = new HashSet<>();
        if (services != null) {
            for (Service old : services) {
                olds.add(old.getName());
                Service update = updateMap.get(old.getName());
                if (update == null) {
                    if (deletes == null || !deletes.contains(old.getName())) {
                        result.add(old);
                    } else {
                        // Delete
                        if (ServiceOp.onDelete(old, merger, owner)) {
                            result.add(old);
                        }
                    }
                } else if (old.getVersion() < update.getVersion()) {
                    // Update
                    ServiceOp.onUpdate(old, update, merger, owner);
                    result.add(old);
                } else {
                    // No change
                    result.add(old);
                }
            }
        }
        for (Service update : updates) {
            if (!olds.contains(update.getName())) {
                // Add
                ServiceOp.onAdd(update, merger, owner);
                result.add(update);
            }
        }
        return result;
    }

    /**
     * Updates the service, using the specified policy merger and owner.
     *
     * @param service The service to update the current services with.
     * @param merger  The policy merger to handle the merging logic.
     * @param owner   The owner of the services.
     * @return The updated list of services.
     */
    public List<Service> onUpdate(Service service, PolicyMerger merger, String owner) {
        return onUpdate(Collections.singletonList(service), null, merger, owner);
    }

    /**
     * Delete the service, using the specified policy merger and owner.
     *
     * @param name   The service name to delete.
     * @param merger The policy merger to handle the merging logic.
     * @param owner  The owner of the services.
     * @return The updated list of services.
     */
    public List<Service> onDelete(String name, PolicyMerger merger, String owner) {
        return onUpdate(new ArrayList<>(), Collections.singleton(name), merger, owner);
    }

    /**
     * Creates a copy of this {@link GovernancePolicy} instance.
     * <p>
     * This method is used for synchronization purposes, where the caches are not copied as they are considered transient.
     * </p>
     *
     * @return A shallow copy of this {@link GovernancePolicy} instance.
     */
    public GovernancePolicy copy() {
        // used in synchronization，cache is useless.
        GovernancePolicy result = new GovernancePolicy();
        result.liveSpaces = liveSpaces;
        result.laneSpaces = laneSpaces;
        result.services = services;
        result.dbClusters = dbClusters;
        return result;
    }
}