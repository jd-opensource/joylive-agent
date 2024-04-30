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

import com.jd.live.agent.core.util.map.ListBuilder;
import com.jd.live.agent.core.util.trie.PathMapTrie;
import com.jd.live.agent.core.util.trie.PathMatchType;
import com.jd.live.agent.core.util.trie.PathTrie;
import com.jd.live.agent.core.util.trie.PathType;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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

    private transient final PathTrie<ServicePath> pathCache = new PathMapTrie<>(new ListBuilder<>(() -> paths, ServicePath::getPath));

    private transient ServiceType serviceType;

    public ServiceGroup() {
    }

    public ServiceGroup(String name) {
        this.name = name;
    }

    public ServiceGroup(String name, boolean defaultGroup, ServicePolicy servicePolicy) {
        this.name = name;
        this.defaultGroup = defaultGroup;
        this.servicePolicy = servicePolicy;
    }

    public ServiceGroup(String name, boolean defaultGroup, ServicePolicy servicePolicy, ServiceType serviceType) {
        this.name = name;
        this.defaultGroup = defaultGroup;
        this.servicePolicy = servicePolicy;
        this.serviceType = serviceType;
    }

    public ServicePath getPath(String name) {
        return getPath(name, null);
    }

    public ServicePath getPath(String name, PathMatchType matchType) {
        PathType pathType = serviceType.getPathType();
        matchType = matchType == null ? serviceType.getMatchType() : matchType;
        switch (matchType) {
            case EQUAL:
                return pathCache.get(name);
            case PREFIX:
            default:
                return pathCache.match(name, pathType.getDelimiter(), pathType.isWithDelimiter());
        }
    }

    public ServicePolicy getServicePolicy(String path, String method) {
        return getServicePolicy(path, method, null);
    }

    public ServicePolicy getServicePolicy(String path, String method, PathMatchType matchType) {
        path = path == null && method != null ? DEFAULT_GROUP : path;
        ServicePath servicePath = getPath(path, matchType);
        return servicePath == null ? servicePolicy : servicePath.getServicePolicy(method);
    }

    public void addPath(ServicePath path) {
        if (path != null) {
            if (paths == null) {
                paths = new ArrayList<>();
            }
            paths.add(path);
        }
    }

    protected void setServiceType(ServiceType serviceType) {
        this.serviceType = serviceType;
    }

    protected void supplement(ServiceGroup source) {
        supplement(source == null ? null : source.getServicePolicy());
        supplementPath(source);
    }

    private void supplementPath(ServiceGroup source) {
        if (source != null && source.paths != null && !source.paths.isEmpty()) {
            List<ServicePath> targets = new ArrayList<>(source.paths.size());
            ServicePath target;
            Set<Long> ids = new HashSet<>();
            for (ServicePath path : source.paths) {
                target = pathCache.get(path.getPath());
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

    private ServicePath supplementPath(ServicePath source, ServicePath target) {
        String path = target.getPath() == null || target.getPath().isEmpty() ? ServicePath.DEFAULT_NAME : target.getPath();
        target.setPath(path);
        target.setServiceType(serviceType);
        target.supplement(() -> addPath(uri, path), supplementTag(KEY_SERVICE_PATH, target.getPath()));
        target.supplement(source == null ? null : source.getServicePolicy());
        target.supplement(servicePolicy);
        target.supplementMethod(source);
        return target;
    }

    protected void cache() {
        super.cache();
        if (paths != null) {
            paths.forEach(ServicePath::cache);
        }
        pathCache.get("");
    }
}
