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
package com.jd.live.agent.plugin.router.springcloud.v3.cluster.context;

import com.jd.live.agent.governance.registry.Registry;
import com.jd.live.agent.governance.registry.ServiceEndpoint;
import com.jd.live.agent.governance.registry.ServiceRegistryFactory;
import com.jd.live.agent.governance.request.ServiceRequest;
import com.jd.live.agent.plugin.router.springcloud.v3.registry.SpringServiceRegistry;
import lombok.Getter;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.*;
import org.springframework.cloud.client.loadbalancer.reactive.ReactiveLoadBalancer;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;

import static com.jd.live.agent.bootstrap.util.type.UnsafeFieldAccessorFactory.getQuietly;

/**
 * Abstract implementation of CloudClusterContext for load balancing and service instance management
 * Provides core functionalities for load balancer property retrieval and service instance suppliers
 */
public abstract class AbstractCloudClusterContext implements CloudClusterContext {

    private static final String FIELD_PROPERTIES = "properties";

    private static final Method METHOD_GET_PROPERTIES = getPropertiesMethod();

    private static final Field FIELD_USE_RAW_STATUS_CODE_IN_RESPONSE_DATA = getUseRawStatusCodeField();

    private static final Map<String, RequestLifecycle> SERVICE_LIFECYCLES = new ConcurrentHashMap<>();

    protected Registry registry;

    protected ServiceRegistryFactory system;

    protected ReactiveLoadBalancer.Factory<ServiceInstance> loadBalancerFactory;

    protected LoadBalancerProperties loadBalancerProperties;

    public AbstractCloudClusterContext(Registry registry) {
        this.registry = registry;
    }

    public ReactiveLoadBalancer.Factory<ServiceInstance> getLoadBalancerFactory() {
        return loadBalancerFactory;
    }

    @Override
    public CompletionStage<List<ServiceEndpoint>> getEndpoints(ServiceRequest request) {
        return registry.getEndpoints(request.getService(), request.getGroup(), system);
    }

    @Override
    public ServiceContext getServiceContext(String service) {
        RequestLifecycle lifecycle = getLifecycle(service);
        LoadBalancerProperties properties = getLoadBalancerProperties(service);
        boolean rawCode = isUseRawStatusCodeInResponseData(properties);
        return new ServiceContext(lifecycle, properties, rawCode);
    }

    protected void setupLoadBalancerFactory(Object target, String... fields) {
        this.loadBalancerFactory = getQuietly(target, fields, v -> v instanceof ReactiveLoadBalancer.Factory);
        this.system = loadBalancerFactory == null ? null : name -> new SpringServiceRegistry(name, loadBalancerFactory);
        this.loadBalancerProperties = getQuietly(loadBalancerFactory, FIELD_PROPERTIES, v -> v instanceof LoadBalancerProperties);
    }

    private RequestLifecycle getLifecycle(String service) {
        return service == null ? null : SERVICE_LIFECYCLES.computeIfAbsent(service, s -> new SpringRequestLifecycle(s, loadBalancerFactory));
    }

    private LoadBalancerProperties getLoadBalancerProperties(String service) {
        try {
            LoadBalancerProperties result = loadBalancerFactory == null || METHOD_GET_PROPERTIES == null ? null : loadBalancerFactory.getProperties(service);
            return result == null ? loadBalancerProperties : result;
        } catch (Throwable e) {
            // fix spring cloud 3.0.6 without getProperties
            return loadBalancerProperties;
        }
    }

    private boolean isUseRawStatusCodeInResponseData(LoadBalancerProperties properties) {
        boolean useRawStatusCodeInResponseData = false;
        if (properties != null && FIELD_USE_RAW_STATUS_CODE_IN_RESPONSE_DATA != null) {
            // fix for spring cloud 2020, without field useRawStatusCodeInResponseData
            useRawStatusCodeInResponseData = properties.isUseRawStatusCodeInResponseData();
        }
        return useRawStatusCodeInResponseData;
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

    private static class SpringRequestLifecycle implements RequestLifecycle {

        @Getter
        private final String service;

        @SuppressWarnings("rawtypes")
        private final Set<LoadBalancerLifecycle> lifecycles;

        SpringRequestLifecycle(String service, ReactiveLoadBalancer.Factory<ServiceInstance> factory) {
            this.service = service;
            this.lifecycles = factory == null ?
                    new HashSet<>() :
                    LoadBalancerLifecycleValidator.getSupportedLifecycleProcessors(
                            factory.getInstances(service, LoadBalancerLifecycle.class),
                            RequestDataContext.class,
                            ResponseData.class,
                            ServiceInstance.class);
        }

        @SuppressWarnings("unchecked")
        @Override
        public void onStart(Request<?> request) {
            if (lifecycles != null) {
                lifecycles.forEach(lifecycle -> lifecycle.onStart(request));
            }
        }

        @SuppressWarnings("unchecked")
        @Override
        public void onStartRequest(Request<?> request, Response<?> lbResponse) {
            if (lifecycles != null) {
                lifecycles.forEach(lifecycle -> lifecycle.onStartRequest(request, lbResponse));
            }
        }

        @SuppressWarnings("unchecked")
        @Override
        public void onComplete(CompletionContext<?, ?, ?> completionContext) {
            if (lifecycles != null) {
                lifecycles.forEach(lifecycle -> lifecycle.onComplete(completionContext));
            }
        }
    }

}
