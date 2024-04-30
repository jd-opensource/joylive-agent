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
import java.util.List;

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
    private List<ServiceGroup> groups;

    @Getter
    private transient ServiceGroup defaultGroup;

    private transient final Cache<String, ServiceGroup> groupCache = new MapCache<>(new ListBuilder<>(() -> groups, ServiceGroup::getName));

    public Service() {
    }

    public Service(String name) {
        this(name, ServiceType.HTTP);
    }

    public Service(String name, ServiceType serviceType) {
        this.name = name;
        this.serviceType = serviceType;
    }

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

    public void addGroup(ServiceGroup group) {
        if (group != null) {
            if (groups == null) {
                groups = new ArrayList<>();
            }
            groups.add(group);
        }
    }

    protected ServiceGroup getGroup(String group) {
        return groupCache.get(group);
    }

    public ServicePolicy getPath(String group, String path, String method) {
        ServiceGroup serviceGroup = getGroup(group);
        serviceGroup = serviceGroup == null ? defaultGroup : serviceGroup;
        return serviceGroup == null ? null : serviceGroup.getServicePolicy(path, method);
    }

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

    public void cache() {
        supplement();
        if (groups != null) {
            groups.forEach(ServiceGroup::cache);
        }
        groupCache.get("");
    }
}
