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
package com.jd.live.agent.plugin.router.springcloud.v2_2.util;

import com.jd.live.agent.bootstrap.util.type.UnsafeFieldAccessor;
import com.netflix.loadbalancer.ILoadBalancer;
import com.netflix.loadbalancer.Server;
import lombok.Getter;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.reactive.*;
import org.springframework.cloud.loadbalancer.core.ReactorServiceInstanceLoadBalancer;
import org.springframework.cloud.loadbalancer.core.ServiceInstanceListSupplier;
import org.springframework.cloud.netflix.ribbon.SpringClientFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;

import static com.jd.live.agent.bootstrap.util.type.UnsafeFieldAccessorFactory.getQuietly;

/**
 * A factory for creating instances of a reactive load balancer that uses the Ribbon client.
 */
public class RibbonLoadBalancerFactory implements ReactiveLoadBalancer.Factory<ServiceInstance> {

    private final SpringClientFactory clientFactory;

    public RibbonLoadBalancerFactory(Object clientFactory) {
        this.clientFactory = (SpringClientFactory) clientFactory;
    }

    @Override
    public ReactiveLoadBalancer<ServiceInstance> getInstance(String serviceId) {
        ILoadBalancer loadBalancer = clientFactory.getLoadBalancer(serviceId);
        // ServiceInstanceSupplier in spring cloud greenwich
        // ServiceInstanceListSupplier in spring cloud hoxton
        return new RandomLoadBalancer(serviceId, new RibbonServiceInstanceListSupplier(serviceId, loadBalancer));
    }

    /**
     * An inner class that supplies a list of service instances using the Ribbon client.
     */
    private static class RibbonServiceInstanceListSupplier implements ServiceInstanceListSupplier {
        private static final String FIELD_METADATA = "metadata";
        private final String serviceId;
        private final ILoadBalancer loadBalancer;

        RibbonServiceInstanceListSupplier(String serviceId, ILoadBalancer loadBalancer) {
            this.serviceId = serviceId;
            this.loadBalancer = loadBalancer;
        }

        @Override
        public String getServiceId() {
            return serviceId;
        }

        @SuppressWarnings("unchecked")
        @Override
        public Flux<List<ServiceInstance>> get() {
            List<Server> servers = loadBalancer.getAllServers();
            List<ServiceInstance> instances = new ArrayList<>(servers.size());
            Function<Server, Map<String, String>> metadataFunc = null;
            for (Server server : servers) {
                if (metadataFunc == null) {
                    UnsafeFieldAccessor accessor = getQuietly(server.getClass(), FIELD_METADATA);
                    metadataFunc = accessor == null ? s -> null : s -> (Map<String, String>) accessor.get(s);
                }
                instances.add(new RibbonServiceInstance(serviceId, server, metadataFunc.apply(server)));
            }
            return Flux.just(instances);
        }
    }

    /**
     * An inner class that represents a service instance using the Ribbon client.
     */
    private static class RibbonServiceInstance implements ServiceInstance {

        private final String serviceId;
        private final Server server;
        private final URI uri;
        private final Map<String, String> metadata;

        RibbonServiceInstance(String serviceId, Server server, Map<String, String> metadata) {
            this.serviceId = serviceId;
            this.server = server;
            String scheme = server.getScheme() == null || server.getScheme().isEmpty() ? "http" : server.getScheme();
            this.uri = URI.create(scheme + "://" + server.getHost() + ":" + server.getPort());
            this.metadata = metadata;
        }

        @Override
        public String getServiceId() {
            return serviceId;
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
        public boolean isSecure() {
            return "https".equals(server.getScheme());
        }

        @Override
        public URI getUri() {
            return uri;
        }

        @Override
        public Map<String, String> getMetadata() {
            return metadata;
        }
    }

    private static class RandomLoadBalancer implements ReactorServiceInstanceLoadBalancer {

        @Getter
        private final String serviceId;

        private final ServiceInstanceListSupplier supplier;

        RandomLoadBalancer(String serviceId, ServiceInstanceListSupplier supplier) {
            this.serviceId = serviceId;
            this.supplier = supplier;
        }

        @SuppressWarnings("deprecation")
        @Override
        public Mono<Response<ServiceInstance>> choose(Request request) {
            return supplier.get().next()
                    .map(instances -> {
                        if (instances.isEmpty()) {
                            return new EmptyResponse();
                        }
                        int index = ThreadLocalRandom.current().nextInt(instances.size());
                        ServiceInstance instance = instances.get(index);
                        return new DefaultResponse(instance);
                    });
        }
    }
}