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
import com.jd.live.agent.core.util.map.ListBuilder;
import com.jd.live.agent.core.util.map.ListBuilder.LowercaseListBuilder;
import com.jd.live.agent.core.util.map.MapBuilder.LowercaseMapBuilder;
import com.jd.live.agent.governance.policy.domain.Domain;
import com.jd.live.agent.governance.policy.domain.DomainPolicy;
import com.jd.live.agent.governance.policy.lane.LaneDomain;
import com.jd.live.agent.governance.policy.lane.LaneSpace;
import com.jd.live.agent.governance.policy.live.LiveDomain;
import com.jd.live.agent.governance.policy.live.LiveSpace;
import com.jd.live.agent.governance.policy.live.LiveSpec;
import com.jd.live.agent.governance.policy.live.UnitDomain;
import com.jd.live.agent.governance.policy.live.db.LiveDatabase;
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

    @Getter
    private transient LiveSpace localLiveSpace;

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

    private final transient Cache<String, LaneSpace> laneSpaceCache = new MapCache<>(new ListBuilder<>(() -> laneSpaces, LaneSpace::getId));

    private final transient Cache<String, Domain> domainCache = new MapCache<>((LowercaseMapBuilder<Domain>) () -> {
        Map<String, Domain> laneDomains = getLaneDomains();
        Map<String, Domain> liveDomains = getLiveDomains();

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

    private final transient Cache<String, Service> serviceCache = new MapCache<>(new LowercaseListBuilder<>(() -> services, null, Service::getName));

    private final transient Cache<String, Service> serviceAliasCache = new MapCache<>(() -> {
        Map<String, Service> result = new HashMap<>();
        if (services != null) {
            services.forEach(service -> {
                if (service.getAliases() != null) {
                    service.getAliases().forEach(alias -> result.put(alias, service));
                }
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
     * Retrieves a {@link Service} by its unique alias.
     *
     * @param alias The unique alias of the service to retrieve.
     * @return The service with the specified alias, or {@code null} if not found.
     */
    public Service getServiceByAlias(String alias) {
        return serviceAliasCache.get(alias);
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

    public LiveDatabase getMaster(String... shards) {
        LiveSpace liveSpace = getLocalLiveSpace();
        LiveSpec spec = liveSpace == null ? null : liveSpace.getSpec();
        if (spec != null) {
            LiveDatabase database = spec.getDatabase(shards);
            if (database != null) {
                return database.getMaster();
            }
        }
        return null;
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
        getLaneSpace("");
        getDomain("");
        getService("");
        getServiceByAlias("");
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
        return result;
    }

    private Map<String, Domain> getLiveDomains() {
        String host;
        Map<String, Domain> liveDomains = new HashMap<>();
        if (liveSpaces != null) {
            for (LiveSpace liveSpace : liveSpaces) {
                LiveSpec liveSpec = liveSpace.getSpec();
                if (liveSpec.getDomains() != null) {
                    for (LiveDomain liveDomain : liveSpec.getDomains()) {
                        host = liveDomain.getHost().toLowerCase();
                        liveDomains.put(host, new Domain(host, new DomainPolicy(liveSpace, liveDomain)));
                        if (liveDomain.getUnitDomains() != null) {
                            for (UnitDomain unitDomain : liveDomain.getUnitDomains()) {
                                host = unitDomain.getHost().toLowerCase();
                                liveDomains.put(host, new Domain(host, new DomainPolicy(liveSpace, liveDomain, unitDomain)));
                            }
                        }
                    }
                }
            }
        }
        return liveDomains;
    }

    private Map<String, Domain> getLaneDomains() {
        String host;
        Map<String, Domain> laneDomains = new HashMap<>();
        if (laneSpaces != null) {
            for (LaneSpace laneSpace : laneSpaces) {
                if (laneSpace.getDomains() != null) {
                    for (LaneDomain laneDomain : laneSpace.getDomains()) {
                        host = laneDomain.getHost().toLowerCase();
                        laneDomains.put(host, new Domain(host, new DomainPolicy(laneSpace, laneDomain)));
                    }
                }
            }
        }
        return laneDomains;
    }
}