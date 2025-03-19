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
package com.jd.live.agent.plugin.router.springcloud.v2_1.instance;

import com.jd.live.agent.bootstrap.util.AbstractAttributes;
import com.jd.live.agent.bootstrap.util.type.UnsafeFieldAccessor;
import com.jd.live.agent.core.util.cache.CacheObject;
import com.jd.live.agent.governance.instance.AbstractEndpoint;
import com.jd.live.agent.governance.instance.EndpointState;
import com.jd.live.agent.governance.registry.ServiceEndpoint;
import com.netflix.loadbalancer.Server;
import org.springframework.cloud.client.ServiceInstance;

import java.net.URI;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.jd.live.agent.bootstrap.util.type.UnsafeFieldAccessorFactory.getQuietly;

/**
 * A class that represents a service endpoint in the context of Ribbon load balancing.
 * This class implements both {@link ServiceEndpoint} and {@link ServiceInstance} interfaces,
 * providing a unified representation of a service endpoint that can be used for load balancing.
 * It extends {@link AbstractAttributes} to support additional attributes associated with the endpoint.
 */
public class RibbonEndpoint extends AbstractEndpoint implements InstanceEndpoint {

    private static final Map<Class<?>, CacheObject<UnsafeFieldAccessor>> ACCESSOR_MAP = new ConcurrentHashMap<>();
    private static final String FIELD_METADATA = "metadata";

    private final String service;

    private final Server server;

    private final String scheme;

    private volatile URI uri;

    private volatile CacheObject<Map<String, String>> metadata;

    public RibbonEndpoint(String service, Server server) {
        this.service = service;
        this.server = server;
        this.scheme = server.getScheme() == null || server.getScheme().isEmpty() ? "http" : server.getScheme();
    }

    @Override
    public String getId() {
        return server.getId();
    }

    @Override
    public String getServiceId() {
        return service;
    }

    @Override
    public String getService() {
        return service;
    }

    @Override
    public String getScheme() {
        return scheme;
    }

    @Override
    public String getHost() {
        return server.getHost();
    }

    @Override
    public int getPort() {
        return server.getPort();
    }

    @Override
    public EndpointState getState() {
        if (!server.isAlive()) {
            return EndpointState.DISABLE;
        } else if (!server.isReadyToServe()) {
            return EndpointState.SUSPEND;
        }
        return EndpointState.HEALTHY;
    }

    @Override
    public boolean isSecure() {
        return "https".equals(scheme);
    }

    @Override
    public URI getUri() {
        if (uri == null) {
            uri = URI.create(scheme + "://" + getHost() + ":" + getPort());
        }
        return uri;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, String> getMetadata() {
        if (metadata == null) {
            Map<String, String> result = null;
            // for nacos
            CacheObject<UnsafeFieldAccessor> cache = ACCESSOR_MAP.computeIfAbsent(server.getClass(), c -> new CacheObject<>(getQuietly(c, FIELD_METADATA)));
            UnsafeFieldAccessor accessor = cache.get();
            if (accessor != null) {
                Object target = accessor.get(server);
                if (target instanceof Map) {
                    result = (Map<String, String>) target;
                }
            }
            metadata = new CacheObject<>(result);
        }
        return metadata.get();
    }
}
