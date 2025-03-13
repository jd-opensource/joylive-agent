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
package com.jd.live.agent.plugin.router.springcloud.v4.cluster.context;

import com.jd.live.agent.bootstrap.util.type.UnsafeFieldAccessorFactory;
import com.jd.live.agent.core.util.cache.CacheObject;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.*;
import org.springframework.cloud.client.loadbalancer.reactive.ReactiveLoadBalancer;
import org.springframework.cloud.loadbalancer.core.ServiceInstanceListSupplier;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Abstract implementation of CloudClusterContext for load balancing and service instance management
 * Provides core functionalities for load balancer property retrieval and service instance suppliers
 */
public abstract class AbstractCloudClusterContext implements CloudClusterContext {

    private static final Method METHOD_GET_PROPERTIES = getPropertiesMethod();

    private static final Field FIELD_USE_RAW_STATUS_CODE_IN_RESPONSE_DATA = getUseRawStatusCodeField();

    private static final Map<String, CacheObject<ServiceInstanceListSupplier>> SERVICE_INSTANCE_LIST_SUPPLIERS = new ConcurrentHashMap<>();

    @SuppressWarnings("rawtypes")
    private static final Map<String, Set<LoadBalancerLifecycle>> LOAD_BALANCER_LIFE_CYCLES = new ConcurrentHashMap<>();

    private static final String FIELD_SERVICE_INSTANCE_LIST_SUPPLIER_PROVIDER = "serviceInstanceListSupplierProvider";

    protected ReactiveLoadBalancer.Factory<ServiceInstance> loadBalancerFactory;

    protected LoadBalancerProperties loadBalancerProperties;

    public AbstractCloudClusterContext() {
    }

    public AbstractCloudClusterContext(ReactiveLoadBalancer.Factory<ServiceInstance> loadBalancerFactory,
                                       LoadBalancerProperties loadBalancerProperties) {
        this.loadBalancerFactory = loadBalancerFactory;
        this.loadBalancerProperties = loadBalancerProperties;
    }

    public ReactiveLoadBalancer.Factory<ServiceInstance> getLoadBalancerFactory() {
        return loadBalancerFactory;
    }

    @Override
    public LoadBalancerProperties getLoadBalancerProperties(String service) {
        try {
            LoadBalancerProperties result = loadBalancerFactory == null || METHOD_GET_PROPERTIES == null ? null : loadBalancerFactory.getProperties(service);
            return result == null ? loadBalancerProperties : result;
        } catch (Throwable e) {
            // fix spring cloud 3.0.6 without getProperties
            return loadBalancerProperties;
        }
    }

    @Override
    public ServiceInstanceListSupplier getServiceInstanceListSupplier(String service) {
        return SERVICE_INSTANCE_LIST_SUPPLIERS.computeIfAbsent(service, n -> {
            ServiceInstanceListSupplier supplier = null;
            ReactiveLoadBalancer<ServiceInstance> loadBalancer = loadBalancerFactory == null ? null : loadBalancerFactory.getInstance(n);
            if (loadBalancer != null) {
                ObjectProvider<ServiceInstanceListSupplier> provider = UnsafeFieldAccessorFactory.getQuietly(loadBalancer, FIELD_SERVICE_INSTANCE_LIST_SUPPLIER_PROVIDER);
                supplier = provider == null ? null : provider.getIfAvailable();
            }
            return CacheObject.of(supplier);
        }).get();
    }

    @Override
    @SuppressWarnings("rawtypes")
    public Set<LoadBalancerLifecycle> getLifecycleProcessors(String service) {
        return LOAD_BALANCER_LIFE_CYCLES.computeIfAbsent(service, n -> loadBalancerFactory == null ?
                new HashSet<>() :
                LoadBalancerLifecycleValidator.getSupportedLifecycleProcessors(
                        loadBalancerFactory.getInstances(n, LoadBalancerLifecycle.class),
                        RequestDataContext.class,
                        ResponseData.class,
                        ServiceInstance.class));
    }

    /**
     * Retrieves the field for raw status code usage in response data
     *
     * @return Field representing useRawStatusCodeInResponseData, or null if not found
     */
    private static Field getUseRawStatusCodeField() {
        Field field = null;
        try {
            field = LoadBalancerProperties.class.getDeclaredField("useRawStatusCodeInResponseData");
        } catch (NoSuchFieldException ignored) {
        }
        return field;
    }

    /**
     * Retrieves the method to get load balancer properties
     *
     * @return Method to get properties, or null if not found
     */
    private static Method getPropertiesMethod() {
        Method method = null;
        try {
            method = ReactiveLoadBalancer.Factory.class.getDeclaredMethod("getProperties", String.class);
        } catch (NoSuchMethodException ignored) {
        }
        return method;
    }

}
