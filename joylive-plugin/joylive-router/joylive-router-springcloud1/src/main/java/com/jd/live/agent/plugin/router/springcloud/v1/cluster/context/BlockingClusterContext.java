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
package com.jd.live.agent.plugin.router.springcloud.v1.cluster.context;

import com.jd.live.agent.bootstrap.util.type.FieldAccessor;
import com.jd.live.agent.bootstrap.util.type.FieldAccessorFactory;
import com.jd.live.agent.governance.registry.Registry;
import com.jd.live.agent.governance.registry.ServiceRegistry;
import com.jd.live.agent.governance.registry.ServiceRegistryFactory;
import com.jd.live.agent.plugin.router.springcloud.v1.registry.RibbonServiceRegistry;
import lombok.Getter;
import org.springframework.cloud.client.loadbalancer.*;
import org.springframework.cloud.netflix.ribbon.RibbonLoadBalancerClient;
import org.springframework.cloud.netflix.ribbon.SpringClientFactory;
import org.springframework.http.client.ClientHttpRequestInterceptor;

/**
 * A concrete implementation of cluster context that provides blocking behavior
 * for cluster operations, extending {@link AbstractCloudClusterContext}
 */
public class BlockingClusterContext extends AbstractCloudClusterContext {

    private static final String TYPE_RIBBON_LOAD_BALANCER_CLIENT = "org.springframework.cloud.netflix.ribbon.RibbonLoadBalancerClient";

    private final LoadBalancerRetryProperties retryProperties;

    @Getter
    private final LoadBalancerRequestFactory requestFactory;

    public BlockingClusterContext(Registry registry, LoadBalancerInterceptor interceptor) {
        super(registry);
        // LoadBalancerInterceptor.requestFactory
        this.requestFactory = (LoadBalancerRequestFactory) LoadBalancerInterceptorAccessor.requestFactory.get(interceptor);
        // LoadBalancerInterceptor.loadBalancer
        this.system = createFactory((LoadBalancerClient) LoadBalancerInterceptorAccessor.loadBalancer.get(interceptor));
        this.retryProperties = null;
        this.retryFactory = null;
    }

    public BlockingClusterContext(Registry registry, RetryLoadBalancerInterceptor interceptor) {
        super(registry);
        // RetryLoadBalancerInterceptor.requestFactory
        this.requestFactory = (LoadBalancerRequestFactory) RetryLoadBalancerInterceptorAccessor.requestFactory.get(interceptor);
        // RetryLoadBalancerInterceptor.loadBalancer
        this.system = createFactory((LoadBalancerClient) RetryLoadBalancerInterceptorAccessor.loadBalancer.get(interceptor));
        // RetryLoadBalancerInterceptor.lbProperties
        this.retryProperties = (LoadBalancerRetryProperties) RetryLoadBalancerInterceptorAccessor.lbProperties.get(interceptor);
        // RetryLoadBalancerInterceptor.lbRetryFactory
        this.retryFactory = (LoadBalancedRetryPolicyFactory) RetryLoadBalancerInterceptorAccessor.lbRetryPolicyFactory.get(interceptor);
    }

    /**
     * Creates a BlockingClusterContext instance based on the interceptor type.
     *
     * @param registry    the service registry
     * @param interceptor the HTTP request interceptor
     * @return BlockingClusterContext instance if interceptor is supported, null otherwise
     */
    public static BlockingClusterContext of(Registry registry, ClientHttpRequestInterceptor interceptor) {
        if (interceptor instanceof RetryLoadBalancerInterceptor) {
            return new BlockingClusterContext(registry, (RetryLoadBalancerInterceptor) interceptor);
        }
        return new BlockingClusterContext(registry, (LoadBalancerInterceptor) interceptor);
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
        if (name.equals(TYPE_RIBBON_LOAD_BALANCER_CLIENT)) {
            // RibbonLoadBalancerClient.clientFactory
            // SpringClientFactory
            return service -> new RibbonServiceRegistry(service, (SpringClientFactory) RibbonLoadBalancerClientAccessor.clientFactory.get(client));
        }
        return null;
    }

    private static class LoadBalancerInterceptorAccessor {

        private static final FieldAccessor requestFactory = FieldAccessorFactory.getAccessor(LoadBalancerInterceptor.class, "requestFactory");

        private static final FieldAccessor loadBalancer = FieldAccessorFactory.getAccessor(LoadBalancerInterceptor.class, "loadBalancer");

    }

    private static class RetryLoadBalancerInterceptorAccessor {

        private static final FieldAccessor requestFactory = FieldAccessorFactory.getAccessor(RetryLoadBalancerInterceptor.class, "requestFactory");

        private static final FieldAccessor loadBalancer = FieldAccessorFactory.getAccessor(RetryLoadBalancerInterceptor.class, "loadBalancer");

        private static final FieldAccessor lbProperties = FieldAccessorFactory.getAccessor(RetryLoadBalancerInterceptor.class, "lbProperties");

        private static final FieldAccessor lbRetryPolicyFactory = FieldAccessorFactory.getAccessor(RetryLoadBalancerInterceptor.class, "lbRetryPolicyFactory");

    }

    private static class RibbonLoadBalancerClientAccessor {

        private static final FieldAccessor clientFactory = FieldAccessorFactory.getAccessor(RibbonLoadBalancerClient.class, "clientFactory");

    }

}
