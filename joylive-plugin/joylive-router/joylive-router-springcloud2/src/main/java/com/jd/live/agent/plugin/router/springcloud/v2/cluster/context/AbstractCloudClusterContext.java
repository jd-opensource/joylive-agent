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
package com.jd.live.agent.plugin.router.springcloud.v2.cluster.context;

import com.jd.live.agent.core.util.cache.CacheObject;
import com.jd.live.agent.governance.policy.service.cluster.RetryPolicy;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.reactive.ReactiveLoadBalancer;
import org.springframework.cloud.loadbalancer.core.ServiceInstanceListSupplier;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.jd.live.agent.bootstrap.util.type.UnsafeFieldAccessorFactory.getQuietly;

/**
 * Abstract implementation of CloudClusterContext for load balancing and service instance management
 * Provides core functionalities for load balancer property retrieval and service instance suppliers
 */
public abstract class AbstractCloudClusterContext implements CloudClusterContext {

    private static final Map<String, CacheObject<ServiceInstanceListSupplier>> SERVICE_INSTANCE_LIST_SUPPLIERS = new ConcurrentHashMap<>();

    private static final String FIELD_SERVICE_INSTANCE_LIST_SUPPLIER_PROVIDER = "serviceInstanceListSupplierProvider";

    protected ReactiveLoadBalancer.Factory<ServiceInstance> loadBalancerFactory;

    protected RetryPolicy defaultRetryPolicy;

    public AbstractCloudClusterContext() {
    }

    public AbstractCloudClusterContext(ReactiveLoadBalancer.Factory<ServiceInstance> loadBalancerFactory, RetryPolicy defaultRetryPolicy) {
        this.loadBalancerFactory = loadBalancerFactory;
        this.defaultRetryPolicy = defaultRetryPolicy;
    }

    public ReactiveLoadBalancer.Factory<ServiceInstance> getLoadBalancerFactory() {
        return loadBalancerFactory;
    }

    @Override
    public boolean isRetryable() {
        return getDefaultRetryPolicy() != null;
    }

    @Override
    public RetryPolicy getDefaultRetryPolicy() {
        return defaultRetryPolicy;
    }

    @Override
    public ServiceInstanceListSupplier getServiceInstanceListSupplier(String service) {
        return SERVICE_INSTANCE_LIST_SUPPLIERS.computeIfAbsent(service, n -> {
            ServiceInstanceListSupplier supplier = null;
            ReactiveLoadBalancer<ServiceInstance> loadBalancer = loadBalancerFactory == null ? null : loadBalancerFactory.getInstance(n);
            if (loadBalancer != null) {
                ObjectProvider<ServiceInstanceListSupplier> provider = getQuietly(loadBalancer, FIELD_SERVICE_INSTANCE_LIST_SUPPLIER_PROVIDER);
                supplier = provider == null ? null : provider.getIfAvailable();
            }
            return CacheObject.of(supplier);
        }).get();
    }

}
