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
package com.jd.live.agent.plugin.router.springcloud.v2_2.registry;

import com.jd.live.agent.governance.registry.ServiceEndpoint;
import com.jd.live.agent.governance.registry.ServiceRegistry;
import com.jd.live.agent.plugin.router.springcloud.v2_2.instance.SpringEndpoint;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.reactive.ReactiveLoadBalancer;
import org.springframework.cloud.client.loadbalancer.reactive.Response;
import org.springframework.cloud.loadbalancer.core.ServiceInstanceListSupplier;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

import static com.jd.live.agent.bootstrap.util.type.UnsafeFieldAccessorFactory.getQuietly;
import static com.jd.live.agent.core.util.CollectionUtils.toList;

/**
 * A {@link ServiceRegistry} implementation for Spring-based service discovery and load balancing.
 * This class integrates with Spring's {@link ReactiveLoadBalancer} to retrieve service endpoints
 * dynamically based on the provided service name and load balancer factory.
 */
public class SpringServiceRegistry implements ServiceRegistry {

    private static final String FIELD_SERVICE_INSTANCE_LIST_SUPPLIER_PROVIDER = "serviceInstanceListSupplierProvider";

    private final String service;

    private final ReactiveLoadBalancer<ServiceInstance> balancer;

    private final ServiceInstanceListSupplier supplier;

    public SpringServiceRegistry(String service, ReactiveLoadBalancer.Factory<ServiceInstance> factory) {
        this.service = service;
        this.balancer = factory.getInstance(service);
        this.supplier = getSupplier(balancer);
    }

    @SuppressWarnings("deprecation")
    @Override
    public List<ServiceEndpoint> getEndpoints() {
        if (balancer == null) {
            return null;
        }
        if (supplier != null) {
            List<ServiceInstance> instances = supplier.get().blockFirst();
            return toList(instances, s -> new SpringEndpoint(service, s));
        }
        Response<ServiceInstance> loadBalancerResponse = Mono.from(balancer.choose()).block();
        if (loadBalancerResponse == null) {
            return null;
        }
        List<ServiceEndpoint> result = new ArrayList<>();
        result.add(new SpringEndpoint(service, loadBalancerResponse.getServer()));
        return result;
    }

    @Override
    public String getService() {
        return service;
    }

    /**
     * Retrieves the {@link ServiceInstanceListSupplier} from the provided load balancer.
     * This method attempts to extract the supplier from the load balancer using reflection.
     * If the supplier is not available or an exception occurs, it returns {@code null}.
     *
     * @param balancer the {@link ReactiveLoadBalancer} from which to retrieve the supplier
     * @return the {@link ServiceInstanceListSupplier}, or {@code null} if not available
     */
    private static ServiceInstanceListSupplier getSupplier(ReactiveLoadBalancer<ServiceInstance> balancer) {
        // RoundRobinLoadBalancer
        // NacosLoadBalancer
        // RoundRobinLoadBalancer
        ObjectProvider<ServiceInstanceListSupplier> provider = getQuietly(balancer, FIELD_SERVICE_INSTANCE_LIST_SUPPLIER_PROVIDER);
        try {
            return provider == null ? null : provider.getIfAvailable();
        } catch (BeansException e) {
            return null;
        }
    }
}
