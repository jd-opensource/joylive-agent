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

import com.jd.live.agent.core.instance.AppService;
import com.jd.live.agent.core.instance.Application;
import com.jd.live.agent.core.instance.Location;
import com.jd.live.agent.core.util.URI;
import com.jd.live.agent.core.util.cache.Cache;
import com.jd.live.agent.core.util.cache.MapCache;
import com.jd.live.agent.core.util.cache.UnsafeLazyObject;
import com.jd.live.agent.core.util.map.CaseInsensitiveMap;
import com.jd.live.agent.core.util.map.ListBuilder;
import com.jd.live.agent.governance.policy.domain.Domain;
import com.jd.live.agent.governance.policy.domain.DomainPolicy;
import com.jd.live.agent.governance.policy.lane.Lane;
import com.jd.live.agent.governance.policy.lane.LaneDomain;
import com.jd.live.agent.governance.policy.lane.LaneSpace;
import com.jd.live.agent.governance.policy.live.*;
import com.jd.live.agent.governance.policy.live.db.LiveDatabase;
import com.jd.live.agent.governance.policy.live.db.LiveDatabaseSpec;
import com.jd.live.agent.governance.policy.live.db.LiveDatabaseSupervisor;
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
public class GovernancePolicy implements LiveDatabaseSupervisor {

    @Setter
    @Getter
    private List<LiveSpace> liveSpaces;

    @Setter
    @Getter
    private List<LiveDatabaseSpec> databaseSpecs;

    @Getter
    @Setter
    private List<LaneSpace> laneSpaces;

    @Setter
    @Getter
    private List<Service> services;

    @Getter
    private transient LiveSpace localLiveSpace;

    @Getter
    private transient LiveDatabaseSpec localDatabaseSpec;

    @Getter
    private transient LaneSpace localLaneSpace;

    @Getter
    private transient Service localService;

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

    private final transient Cache<String, LiveSpace> liveSpaceCache = new MapCache<>(new ListBuilder<>(() -> liveSpaces, LiveSpace::getId));

    private final transient Cache<String, LiveDatabaseSpec> databaseCache = new MapCache<>(new ListBuilder<>(() -> databaseSpecs, LiveDatabaseSpec::getId));

    private final transient Cache<String, LaneSpace> laneSpaceCache = new MapCache<>(new ListBuilder<>(() -> laneSpaces, LaneSpace::getId));

    private final transient Cache<String, Domain> domainCache = new MapCache<>(() -> {
        // CaseInsensitiveMap
        Map<String, Domain> laneDomains = getLaneDomains();
        Map<String, Domain> liveDomains = getLiveDomains();
        if (liveDomains.isEmpty()) {
            return laneDomains;
        } else if (laneDomains.isEmpty()) {
            return liveDomains;
        } else {
            Map<String, Domain> result = new CaseInsensitiveMap<>(Integer.max(liveDomains.size(), laneDomains.size()));
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
        }
    });

    private final transient Cache<String, Service> serviceCache = new MapCache<>(() -> {
        int size = services == null ? 0 : services.size();
        Map<String, Service> result = new CaseInsensitiveMap<>(size);
        if (services != null) {
            services.forEach(service -> {
                result.putIfAbsent(service.getName(), service);
                service.alias(alias -> result.putIfAbsent(alias, service));
            });
        }
        return result;
    });

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
     * @param laneSpaces The list of lane spaces governed by this policy.
     */
    public GovernancePolicy(List<LiveSpace> liveSpaces, List<Service> services, List<LaneSpace> laneSpaces) {
        this.liveSpaces = liveSpaces;
        this.services = services;
        this.laneSpaces = laneSpaces;
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

    public Unit getLocalUnit() {
        return localLiveSpace == null ? null : localLiveSpace.getLocalUnit();
    }

    public Cell getLocalCell() {
        return localLiveSpace == null ? null : localLiveSpace.getLocalCell();
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

    public Lane getLocalLane() {
        return localLaneSpace == null ? null : localLaneSpace.getCurrentLane();
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
     * Checks if subdomain is enabled for the given host.
     *
     * @param host the host to check
     * @return true if unit domain is enabled in live domain policy, false otherwise
     */
    public boolean isSubdomainEnabled(String host) {
        Domain domain = getDomain(host);
        if (domain != null) {
            DomainPolicy domainPolicy = domain.getPolicy();
            if (domainPolicy != null) {
                LiveDomain liveDomain = domainPolicy.getLiveDomain();
                if (liveDomain != null && liveDomain.isUnitDomainEnabled()) {
                    return true;
                }
            }
        }
        return false;
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
     * Retrieves a service by name(s). Checks each name in order and returns
     * the first matching service found in the cache. Returns null if none found.
     *
     * @param names one or more service names to look up (nullable)
     * @return first matching service, or null if no matches found
     */
    public Service getService(String... names) {
        int length = names == null ? 0 : names.length;
        switch (length) {
            case 0:
                return null;
            case 1:
                return serviceCache.get(names[0]);
            default:
                Service service;
                for (String name : names) {
                    service = serviceCache.get(name);
                    if (service != null) {
                        return service;
                    }
                }
                return null;
        }
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

    public LiveDatabaseSpec getLiveDatabaseSpec(String id) {
        return databaseCache.get(id);
    }

    @Override
    public LiveDatabase getDatabase(String address) {
        return localDatabaseSpec == null ? null : localDatabaseSpec.getDatabase(address);
    }

    @Override
    public LiveDatabase getDatabase(String[] shards) {
        return localDatabaseSpec == null ? null : localDatabaseSpec.getDatabase(shards);
    }

    @Override
    public LiveDatabase getWriteDatabase(String... shards) {
        return localDatabaseSpec == null ? null : localDatabaseSpec.getWriteDatabase(shards);
    }

    @Override
    public LiveDatabase getReadDatabase(String unit, String cell, String... shards) {
        return localDatabaseSpec == null ? null : localDatabaseSpec.getReadDatabase(unit, cell, shards);
    }

    /**
     * Locates the given application in the live space and lane space.
     *
     * @param application the application object containing the location information
     */
    public void locate(Application application) {
        if (application == null) {
            return;
        }
        Location location = application.getLocation();
        localLiveSpace = getLiveSpace(location.getLiveSpaceId());
        if (localLiveSpace != null) {
            localLiveSpace.locate(location.getUnit(), location.getCell());
        }
        localDatabaseSpec = getLiveDatabaseSpec(location.getLiveSpaceId());
        localLaneSpace = getLaneSpace(location.getLaneSpaceId());
        if (localLaneSpace != null) {
            localLaneSpace.locate(location.getLane());
        }
        AppService appService = application.getService();
        localService = getService(appService.getName());
    }

    /**
     * Populates the caches with initial values.
     * <p>
     * This method should be called to populate the caches after initialization or when the underlying data changes.
     * </p>
     */
    public void cache() {
        getLiveSpace("");
        getLiveDatabaseSpec("");
        getLaneSpace("");
        getDomain("");
        getService("");
        getDefaultLaneSpace();

        if (liveSpaces != null) {
            liveSpaces.forEach(LiveSpace::cache);
        }
        if (databaseSpecs != null) {
            databaseSpecs.forEach(LiveDatabaseSpec::cache);
        }
        if (laneSpaces != null) {
            laneSpaces.forEach(LaneSpace::cache);
        }
        if (services != null) {
            services.forEach(Service::cache);
        }
    }

    /**
     * Updates services from a specific policy model. Services are aggregated by service name
     * across different policy models. This method handles updates from one policy model source.
     *
     * @param updates The list of services to be updated from this policy model
     * @param deletes The set of service names to be removed from this policy model
     * @param merger The policy merger to handle the merging logic across different policy models
     * @param owner The identifier of the policy model source
     * @return The list of services after applying updates from this policy model
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
                    if (deletes == null || !deletes.contains(old.getName()) || ServiceOp.onDelete(old, merger, owner)) {
                        // Not deleted, or still has other policy models after deletion
                        result.add(old);
                    }
                } else if (old.getVersion() != update.getVersion()) {
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
     * Updates a single service from a policy model.
     *
     * @param service The service to update
     * @param merger The policy merger to handle merging
     * @param owner The policy model identifier
     * @return The list of updated services
     */
    public List<Service> onUpdate(Service service, PolicyMerger merger, String owner) {
        return onUpdate(Collections.singletonList(service), null, merger, owner);
    }

    /**
     * Deletes a service from a policy model.
     *
     * @param name The service name to delete
     * @param merger The policy merger to handle merging
     * @param owner The policy model identifier
     * @return The list of remaining services
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
        result.databaseSpecs = databaseSpecs;
        return result;
    }

    private Map<String, Domain> getLiveDomains() {
        String host;
        Map<String, Domain> result = new CaseInsensitiveMap<>();
        if (liveSpaces != null) {
            for (LiveSpace liveSpace : liveSpaces) {
                LiveSpec liveSpec = liveSpace.getSpec();
                if (liveSpec.getDomains() != null) {
                    for (LiveDomain liveDomain : liveSpec.getDomains()) {
                        host = liveDomain.getHost().toLowerCase();
                        result.put(host, new Domain(host, new DomainPolicy(liveSpace, liveDomain)));
                        if (liveDomain.getUnitDomains() != null) {
                            for (UnitDomain unitDomain : liveDomain.getUnitDomains()) {
                                host = unitDomain.getHost().toLowerCase();
                                result.put(host, new Domain(host, new DomainPolicy(liveSpace, liveDomain, unitDomain)));
                            }
                        }
                    }
                }
            }
        }
        return result;
    }

    private Map<String, Domain> getLaneDomains() {
        String host;
        Map<String, Domain> result = new CaseInsensitiveMap<>();
        if (laneSpaces != null) {
            for (LaneSpace laneSpace : laneSpaces) {
                if (laneSpace.getDomains() != null) {
                    for (LaneDomain laneDomain : laneSpace.getDomains()) {
                        host = laneDomain.getHost().toLowerCase();
                        result.put(host, new Domain(host, new DomainPolicy(laneSpace, laneDomain)));
                    }
                }
            }
        }
        return result;
    }
}