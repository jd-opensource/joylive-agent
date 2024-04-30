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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * ServicePath
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

    public ServiceMethod getMethod(String name) {
        return methodCache.get(name);
    }

    public ServicePolicy getServicePolicy(String method) {
        ServiceMethod serviceMethod = getMethod(method);
        return serviceMethod == null ? servicePolicy : serviceMethod.getServicePolicy();
    }

    public void addMethod(ServiceMethod method) {
        if (methods == null) {
            methods = new ArrayList<>();
        }
        methods.add(method);
    }

    protected void setServiceType(ServiceType serviceType) {
        this.serviceType = serviceType;
    }

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

    private ServiceMethod supplementMethod(ServiceMethod source, ServiceMethod target) {
        target.supplement(() -> addPath(uri, target.getName()),
                supplementTag(ServiceMethod.KEY_SERVICE_METHOD, target.getName()));
        target.supplement(source == null ? null : source.getServicePolicy());
        target.supplement(servicePolicy);
        return target;
    }

    protected void cache() {
        super.cache();
        if (methods != null) {
            methods.forEach(ServiceMethod::cache);
        }
        methodCache.get("");
    }
}
