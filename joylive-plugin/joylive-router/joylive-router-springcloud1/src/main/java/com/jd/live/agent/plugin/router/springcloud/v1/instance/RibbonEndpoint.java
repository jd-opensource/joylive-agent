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
package com.jd.live.agent.plugin.router.springcloud.v1.instance;

import com.jd.live.agent.bootstrap.util.type.FieldAccessor;
import com.jd.live.agent.core.util.cache.CacheObject;
import com.jd.live.agent.governance.instance.AbstractEndpoint;
import com.jd.live.agent.governance.instance.EndpointState;
import com.jd.live.agent.governance.registry.ServiceEndpoint;
import com.netflix.loadbalancer.Server;
import lombok.Getter;
import org.springframework.cloud.client.ServiceInstance;

import java.net.URI;
import java.util.Map;

import static com.jd.live.agent.bootstrap.util.type.FieldAccessorFactory.getAccessor;
import static com.jd.live.agent.core.util.type.ClassUtils.loadClass;

/**
 * A class that represents a service endpoint in the context of Ribbon load balancing.
 */
public class RibbonEndpoint extends AbstractEndpoint implements ServiceEndpoint, ServiceInstance {

    public static final String ATTRIBUTE_CLIENT_REQUEST = "clientRequest";

    private final String service;

    @Getter
    private final Server server;

    private final String scheme;

    private volatile URI uri;

    private volatile CacheObject<Map<String, String>> metadata;

    public RibbonEndpoint(String service, Server server) {
        this.service = service;
        this.server = server;
        this.scheme = server.getScheme() == null || server.getScheme().isEmpty() ? DEFAULT_HTTP_SCHEME : server.getScheme();
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
        return SECURE_SCHEME.test(scheme);
    }

    @Override
    public URI getUri() {
        if (uri == null) {
            uri = URI.create(scheme + "://" + getHost() + ":" + getPort());
        }
        return uri;
    }

    @Override
    public Map<String, String> getMetadata() {
        if (metadata == null) {
            metadata = new CacheObject<>(Accessor.getMetadata(server));
        }
        return metadata.get();
    }

    private static class Accessor {
        // for nacos
        private static final Class<?> nacosType = loadClass("com.alibaba.cloud.nacos.ribbon.NacosServer", Server.class.getClassLoader());
        private static final FieldAccessor nacosMetadata = getAccessor(nacosType, "metadata");
        // for eureka
        private static final Class<?> eurekaType = loadClass("com.netflix.niws.loadbalancer.DiscoveryEnabledServer", Server.class.getClassLoader());
        private static final FieldAccessor instanceInfo = getAccessor(eurekaType, "instanceInfo");
        private static final Class<?> instanceInfoType = loadClass("com.netflix.appinfo.InstanceInfo", Server.class.getClassLoader());
        private static final FieldAccessor eurekaMetadata = getAccessor(instanceInfoType, "metadata");

        @SuppressWarnings("unchecked")
        public static Map<String, String> getMetadata(Server server) {
            if (nacosType != null && nacosType.isInstance(server)) {
                return (Map<String, String>) nacosMetadata.get(server);
            } else if (eurekaType != null && eurekaType.isInstance(server)) {
                Object info = instanceInfo.get(server);
                if (instanceInfoType != null && instanceInfoType.isInstance(info)) {
                    return (Map<String, String>) eurekaMetadata.get(info);
                }
            }
            return null;
        }

    }

}
