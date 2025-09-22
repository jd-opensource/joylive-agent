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
package com.jd.live.agent.plugin.router.springcloud.v5.cluster.context;

import com.jd.live.agent.governance.registry.Registry;
import com.jd.live.agent.governance.registry.ServiceEndpoint;
import com.jd.live.agent.governance.registry.ServiceRegistryFactory;
import com.jd.live.agent.governance.request.ServiceRequest;
import com.jd.live.agent.plugin.router.springcloud.v5.registry.SpringServiceRegistry;
import lombok.Getter;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.*;
import org.springframework.cloud.client.loadbalancer.reactive.ReactiveLoadBalancer;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;

import static com.jd.live.agent.bootstrap.util.type.FieldAccessorFactory.getQuietly;

/**
 * Abstract implementation of CloudClusterContext for load balancing and service instance management
 * Provides core functionalities for load balancer property retrieval and service instance suppliers
 */
public abstract class AbstractCloudClusterContext implements CloudClusterContext {

    private static final String FIELD_PROPERTIES = "properties";

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
        return new ServiceContext(getLifecycle(service), getLoadBalancerProperties(service));
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
        LoadBalancerProperties result = loadBalancerFactory == null ? null : loadBalancerFactory.getProperties(service);
        return result == null ? loadBalancerProperties : result;
    }

    private static class SpringRequestLifecycle implements RequestLifecycle {

        @Getter
        private final String service;

        @SuppressWarnings("rawtypes")
        private final Set<LoadBalancerLifecycle> lifecycles;

        SpringRequestLifecycle(String service, ReactiveLoadBalancer.Factory<ServiceInstance> loadBalancerFactory) {
            this.service = service;
            this.lifecycles = loadBalancerFactory == null ?
                    new HashSet<>() :
                    LoadBalancerLifecycleValidator.getSupportedLifecycleProcessors(
                            loadBalancerFactory.getInstances(service, LoadBalancerLifecycle.class),
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
