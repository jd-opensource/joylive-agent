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
package com.jd.live.agent.plugin.router.springcloud.v2.request;

import com.jd.live.agent.core.util.cache.CacheObject;
import com.jd.live.agent.core.util.cache.UnsafeLazyObject;
import com.jd.live.agent.governance.request.AbstractHttpRequest.AbstractHttpOutboundRequest;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.reactive.ReactiveLoadBalancer;
import org.springframework.cloud.loadbalancer.core.ServiceInstanceListSupplier;
import org.springframework.http.HttpCookie;
import org.springframework.http.server.reactive.ServerHttpRequest;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.jd.live.agent.core.util.type.ClassUtils.getValue;

/**
 * Represents an outbound HTTP request in a reactive microservices architecture,
 * extending the capabilities of an abstract HTTP outbound request model to include
 * client-specific functionalities. This class encapsulates features such as load balancing,
 * service instance discovery, and lifecycle management, making it suitable for handling
 * dynamic client requests in a distributed system.
 */
public abstract class AbstractClusterRequest<T> extends AbstractHttpOutboundRequest<T> implements SpringClusterRequest {

    protected static final String FIELD_SERVICE_INSTANCE_LIST_SUPPLIER_PROVIDER = "serviceInstanceListSupplierProvider";

    protected static final Map<String, CacheObject<ServiceInstanceListSupplier>> SERVICE_INSTANCE_LIST_SUPPLIERS = new ConcurrentHashMap<>();

    /**
     * A factory for creating instances of {@code ReactiveLoadBalancer} for service instances.
     * This factory is used to obtain a load balancer instance for the service associated with
     * this request.
     */
    protected final ReactiveLoadBalancer.Factory<ServiceInstance> loadBalancerFactory;

    /**
     * A lazy-initialized {@code ServiceInstanceListSupplier} object, responsible for providing
     * a list of available service instances for load balancing.
     */
    protected final UnsafeLazyObject<ServiceInstanceListSupplier> instanceSupplier;

    /**
     * Constructs a new ClientOutboundRequest with the specified parameters.
     *
     * @param request             The original client request to be processed.
     * @param loadBalancerFactory A factory for creating instances of ReactiveLoadBalancer for service instances.
     */
    public AbstractClusterRequest(T request,
                                  ReactiveLoadBalancer.Factory<ServiceInstance> loadBalancerFactory) {
        super(request);
        this.loadBalancerFactory = loadBalancerFactory;
        this.instanceSupplier = new UnsafeLazyObject<>(this::buildServiceInstanceListSupplier);
    }

    @Override
    public String getCookie(String key) {
        if (key == null || key.isEmpty()) {
            return null;
        } else if (request instanceof ServerHttpRequest) {
            ServerHttpRequest httpRequest = (ServerHttpRequest) request;
            HttpCookie cookie = httpRequest.getCookies().getFirst(key);
            return cookie == null ? null : cookie.getValue();
        } else {
            return super.getCookie(key);
        }
    }

    public ServiceInstanceListSupplier getInstanceSupplier() {
        return instanceSupplier.get();
    }

    /**
     * Builds a supplier of service instances for load balancing. This supplier is responsible for providing
     * a list of available service instances that the load balancer can use to distribute the incoming requests.
     * The supplier is obtained from the load balancer instance if it provides one.
     *
     * @return A ServiceInstanceListSupplier that provides a list of available service instances, or null if the
     * load balancer does not provide such a supplier.
     */
    private ServiceInstanceListSupplier buildServiceInstanceListSupplier() {
        return SERVICE_INSTANCE_LIST_SUPPLIERS.computeIfAbsent(getService(), service -> {
            ReactiveLoadBalancer<ServiceInstance> loadBalancer = loadBalancerFactory == null ? null : loadBalancerFactory.getInstance(getService());
            if (loadBalancer != null) {
                ObjectProvider<ServiceInstanceListSupplier> provider = getValue(loadBalancer, FIELD_SERVICE_INSTANCE_LIST_SUPPLIER_PROVIDER);
                return CacheObject.of(provider == null ? null : provider.getIfAvailable());
            }
            return CacheObject.of(null);
        }).get();

    }
}
