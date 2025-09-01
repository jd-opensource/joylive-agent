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
package com.jd.live.agent.plugin.router.springcloud.v2_2.cluster.context;

import com.jd.live.agent.governance.registry.Registry;
import com.jd.live.agent.governance.registry.ServiceRegistry;
import com.jd.live.agent.governance.registry.ServiceRegistryFactory;
import com.jd.live.agent.plugin.router.springcloud.v2_2.registry.RibbonServiceRegistry;
import com.jd.live.agent.plugin.router.springcloud.v2_2.registry.SpringServiceRegistry;
import lombok.Getter;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.cloud.client.loadbalancer.LoadBalancerRequestFactory;
import org.springframework.cloud.client.loadbalancer.LoadBalancerRetryProperties;
import org.springframework.cloud.client.loadbalancer.RetryLoadBalancerInterceptor;
import org.springframework.cloud.client.loadbalancer.reactive.ReactiveLoadBalancer;
import org.springframework.http.client.ClientHttpRequestInterceptor;

import static com.jd.live.agent.bootstrap.util.type.FieldAccessorFactory.getQuietly;

/**
 * A concrete implementation of cluster context that provides blocking behavior
 * for cluster operations, extending {@link AbstractCloudClusterContext}
 */
public class BlockingClusterContext extends AbstractCloudClusterContext {

    private static final String TYPE_BLOCKING_LOAD_BALANCER_CLIENT = "org.springframework.cloud.loadbalancer.blocking.client.BlockingLoadBalancerClient";
    private static final String TYPE_RIBBON_LOAD_BALANCER_CLIENT = "org.springframework.cloud.netflix.ribbon.RibbonLoadBalancerClient";
    private static final String TYPE_LOAD_BALANCER_FEIGN_CLIENT = "org.springframework.cloud.openfeign.ribbon.LoadBalancerFeignClient";

    private static final String FIELD_REQUEST_FACTORY = "requestFactory";
    private static final String FIELD_LOAD_BALANCER = "loadBalancer";
    private static final String FIELD_LOAD_BALANCER_CLIENT_FACTORY = "loadBalancerClientFactory";
    private static final String FIELD_LB_PROPERTIES = "lbProperties";
    private static final String FIELD_CLIENT_FACTORY = "clientFactory";

    private final ClientHttpRequestInterceptor interceptor;

    @Getter
    private final LoadBalancerRequestFactory requestFactory;

    public BlockingClusterContext(Registry registry, ClientHttpRequestInterceptor interceptor) {
        super(registry);
        this.interceptor = interceptor;
        this.requestFactory = getQuietly(interceptor, FIELD_REQUEST_FACTORY);
        this.system = createFactory(getQuietly(interceptor, FIELD_LOAD_BALANCER));
        LoadBalancerRetryProperties retryProperties = getQuietly(interceptor, FIELD_LB_PROPERTIES, v -> v instanceof LoadBalancerRetryProperties);
        this.defaultRetryPolicy = getDefaultRetryPolicy(retryProperties);
    }

    @Override
    public boolean isRetryable() {
        return interceptor instanceof RetryLoadBalancerInterceptor;
    }

    /**
     * Creates a factory for generating {@link ServiceRegistry} instances based on the type of {@link LoadBalancerClient}.
     * This method determines the appropriate factory implementation by inspecting the class name of the provided client.
     *
     * @param client the client used to determine the factory type
     * @return a {@link ServiceRegistryFactory} that creates a {@link ServiceRegistry} for a given service name, or {@code null} if the client is not supported
     */
    public static ServiceRegistryFactory createFactory(Object client) {
        if (client == null) {
            return null;
        }
        String name = client.getClass().getName();
        if (name.equals(TYPE_BLOCKING_LOAD_BALANCER_CLIENT)) {
            // BlockingLoadBalancerClient.loadBalancerClientFactory
            // LoadBalancerClientFactory
            ReactiveLoadBalancer.Factory<ServiceInstance> factory = getQuietly(client, FIELD_LOAD_BALANCER_CLIENT_FACTORY);
            return service -> new SpringServiceRegistry(service, factory);
        } else if (name.equals(TYPE_RIBBON_LOAD_BALANCER_CLIENT)) {
            // RibbonLoadBalancerClient.clientFactory
            // SpringClientFactory
            return service -> new RibbonServiceRegistry(service, getQuietly(client, FIELD_CLIENT_FACTORY));
        } else if (name.equals(TYPE_LOAD_BALANCER_FEIGN_CLIENT)) {
            // RibbonLoadBalancerClient.clientFactory
            // SpringClientFactory
            return service -> new RibbonServiceRegistry(service, getQuietly(client, FIELD_CLIENT_FACTORY));
        }
        return null;
    }
}
