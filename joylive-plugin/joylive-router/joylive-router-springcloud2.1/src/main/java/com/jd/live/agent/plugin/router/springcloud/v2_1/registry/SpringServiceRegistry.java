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
package com.jd.live.agent.plugin.router.springcloud.v2_1.registry;

import com.jd.live.agent.governance.registry.ServiceEndpoint;
import com.jd.live.agent.governance.registry.ServiceRegistry;
import com.jd.live.agent.plugin.router.springcloud.v2_1.instance.SpringEndpoint;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.reactive.ReactiveLoadBalancer;
import org.springframework.cloud.loadbalancer.core.ServiceInstanceSupplier;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import static com.jd.live.agent.bootstrap.util.type.FieldAccessorFactory.getQuietly;
import static com.jd.live.agent.core.util.CollectionUtils.singletonList;
import static com.jd.live.agent.core.util.CollectionUtils.toList;

/**
 * A {@link ServiceRegistry} implementation for Spring-based service discovery and load balancing.
 * This class integrates with Spring's {@link ReactiveLoadBalancer} to retrieve service endpoints
 * dynamically based on the provided service name and load balancer factory.
 */
public class SpringServiceRegistry implements ServiceRegistry {

    private final String service;

    private final ReactiveLoadBalancer<ServiceInstance> balancer;

    private final ServiceInstanceSupplier supplier;

    public SpringServiceRegistry(String service, ReactiveLoadBalancer<ServiceInstance> balancer) {
        this.service = service;
        this.balancer = service == null ? null : balancer;
        this.supplier = getSupplier(balancer);
    }

    @SuppressWarnings("unchecked")
    @Override
    public CompletableFuture<List<ServiceEndpoint>> getEndpoints() {
        if (balancer == null) {
            return CompletableFuture.completedFuture(null);
        }
        Object result;
        if (supplier != null) {
            result = supplier.get().collectList().map(instances -> toList(instances, s -> new SpringEndpoint(service, s))).toFuture();
        } else {
            result = Mono.from(balancer.choose()).map(s -> singletonList(new SpringEndpoint(service, s.getServer()))).toFuture();
        }
        return (CompletableFuture<List<ServiceEndpoint>>) result;
    }

    @Override
    public String getService() {
        return service;
    }

    /**
     * Retrieves the {@link ServiceInstanceSupplier} from the provided load balancer.
     * This method attempts to extract the supplier from the load balancer using reflection.
     * If the supplier is not available or an exception occurs, it returns {@code null}.
     *
     * @param balancer the {@link ReactiveLoadBalancer} from which to retrieve the supplier
     * @return the {@link ServiceInstanceSupplier}, or {@code null} if not available
     */
    private static ServiceInstanceSupplier getSupplier(ReactiveLoadBalancer<ServiceInstance> balancer) {
        // RoundRobinLoadBalancer
        ObjectProvider<ServiceInstanceSupplier> provider = getQuietly(balancer, "serviceInstanceSupplier");
        try {
            return provider == null ? null : provider.getIfAvailable();
        } catch (BeansException e) {
            return null;
        }
    }
}
