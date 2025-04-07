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
package com.jd.live.agent.plugin.router.springcloud.v2_1.cluster.context;

import com.jd.live.agent.governance.registry.ServiceRegistry;
import com.jd.live.agent.governance.registry.ServiceRegistryFactory;
import com.jd.live.agent.plugin.router.springcloud.v2_1.registry.RibbonServiceRegistry;
import com.jd.live.agent.plugin.router.springcloud.v2_1.registry.SpringServiceRegistry;
import lombok.Getter;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.cloud.client.loadbalancer.LoadBalancerRequestFactory;
import org.springframework.cloud.client.loadbalancer.LoadBalancerRetryProperties;
import org.springframework.cloud.client.loadbalancer.reactive.ReactiveLoadBalancer;
import org.springframework.cloud.netflix.ribbon.SpringClientFactory;
import org.springframework.http.client.ClientHttpRequestInterceptor;

import static com.jd.live.agent.bootstrap.util.type.UnsafeFieldAccessorFactory.getQuietly;

/**
 * A concrete implementation of cluster context that provides blocking behavior
 * for cluster operations, extending {@link AbstractCloudClusterContext}
 */
public class BlockingClusterContext extends AbstractCloudClusterContext {

    private static final String TYPE_BLOCKING_LOAD_BALANCER_CLIENT = "org.springframework.cloud.loadbalancer.blocking.client.BlockingLoadBalancerClient";
    private static final String TYPE_RIBBON_LOAD_BALANCER_CLIENT = "org.springframework.cloud.netflix.ribbon.RibbonLoadBalancerClient";
    private static final String FIELD_LOAD_BALANCER = "loadBalancer";
    private static final String FIELD_LB_PROPERTIES = "lbProperties";
    private static final String FIELD_LB_RETRY_FACTORY = "lbRetryFactory";
    private static final String FIELD_LOAD_BALANCER_CLIENT_FACTORY = "loadBalancerClientFactory";
    private static final String FIELD_CLIENT_FACTORY = "clientFactory";
    private static final String FIELD_REQUEST_FACTORY = "requestFactory";

    private LoadBalancerRetryProperties retryProperties;

    @Getter
    private LoadBalancerRequestFactory requestFactory;

    protected BlockingClusterContext() {

    }

    public BlockingClusterContext(ClientHttpRequestInterceptor interceptor) {
        // LoadBalancerInterceptor.requestFactory
        this.requestFactory = getQuietly(interceptor, FIELD_REQUEST_FACTORY);
        // LoadBalancerInterceptor.loadBalancer
        LoadBalancerClient client = getQuietly(interceptor, FIELD_LOAD_BALANCER);
        this.registryFactory = createFactory(client);
        // RetryLoadBalancerInterceptor.lbProperties
        this.retryProperties = getQuietly(interceptor, FIELD_LB_PROPERTIES);
        // RetryLoadBalancerInterceptor.lbRetryFactory
        this.retryFactory = getQuietly(interceptor, FIELD_LB_RETRY_FACTORY);
    }

    @Override
    public boolean isRetryable() {
        return retryProperties != null && retryProperties.isEnabled() && retryFactory != null;
    }

    /**
     * Creates a factory for generating {@link ServiceRegistry} instances based on the type of {@link LoadBalancerClient}.
     * This method determines the appropriate factory implementation by inspecting the class name of the provided client.
     *
     * @param client the {@link LoadBalancerClient} used to determine the factory type
     * @return a {@link ServiceRegistryFactory} that creates a {@link ServiceRegistry} for a given service name, or {@code null} if the client is not supported
     */
    public static ServiceRegistryFactory createFactory(LoadBalancerClient client) {
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
            return service -> new RibbonServiceRegistry(service, (SpringClientFactory) getQuietly(client, FIELD_CLIENT_FACTORY));
        }
        return null;
    }
}
