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

import com.jd.live.agent.bootstrap.logger.Logger;
import com.jd.live.agent.bootstrap.logger.LoggerFactory;
import com.jd.live.agent.bootstrap.util.type.FieldAccessor;
import com.jd.live.agent.governance.registry.Registry;
import com.jd.live.agent.governance.registry.ServiceRegistry;
import com.jd.live.agent.governance.registry.ServiceRegistryFactory;
import com.jd.live.agent.plugin.router.springcloud.v2_1.registry.RibbonServiceRegistry;
import com.jd.live.agent.plugin.router.springcloud.v2_1.registry.SpringServiceRegistry;
import lombok.Getter;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.*;
import org.springframework.cloud.client.loadbalancer.reactive.ReactiveLoadBalancer;
import org.springframework.cloud.netflix.ribbon.SpringClientFactory;
import org.springframework.http.client.ClientHttpRequestInterceptor;

import java.lang.reflect.Method;

import static com.jd.live.agent.bootstrap.util.type.FieldAccessorFactory.getAccessor;
import static com.jd.live.agent.core.util.type.ClassUtils.getDeclaredMethod;
import static com.jd.live.agent.core.util.type.ClassUtils.loadClass;

/**
 * A concrete implementation of cluster context that provides blocking behavior
 * for cluster operations, extending {@link AbstractCloudClusterContext}
 */
public class BlockingClusterContext extends AbstractCloudClusterContext {

    private static final Logger logger = LoggerFactory.getLogger(BlockingClusterContext.class);

    private final LoadBalancerRetryProperties retryProperties;

    @Getter
    private final LoadBalancerRequestFactory requestFactory;

    protected BlockingClusterContext(Registry registry) {
        super(registry);
        this.requestFactory = null;
        this.system = null;
        this.retryProperties = null;
        this.retryFactory = null;
    }

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
        this.retryFactory = (LoadBalancedRetryFactory) RetryLoadBalancerInterceptorAccessor.lbRetryFactory.get(interceptor);
    }

    @Override
    public boolean isRetryable() {
        return retryProperties != null && retryProperties.isEnabled() && retryFactory != null;
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

    /**
     * Creates a factory for generating {@link ServiceRegistry} instances based on the type of {@link LoadBalancerClient}.
     * This method determines the appropriate factory implementation by inspecting the class name of the provided client.
     *
     * @param client the {@link LoadBalancerClient} used to determine the factory type
     * @return a {@link ServiceRegistryFactory} that creates a {@link ServiceRegistry} for a given service name, or {@code null} if the client is not supported
     */
    @SuppressWarnings("unchecked")
    public static ServiceRegistryFactory createFactory(LoadBalancerClient client) {
        if (client == null) {
            return null;
        }
        Class<?> type = client.getClass();
        if (type == BlockingLoadBalancerClientAccessor.clientType) {
            // BlockingLoadBalancerClient.loadBalancerClientFactory
            return service -> new SpringServiceRegistry(service, BlockingLoadBalancerClientAccessor.getLoadBalancer(client, service));
        } else if (type == RibbonLoadBalancerClientAccessor.clientType) {
            // RibbonLoadBalancerClient.clientFactory
            // SpringClientFactory
            return service -> new RibbonServiceRegistry(service, (SpringClientFactory) RibbonLoadBalancerClientAccessor.clientFactory.get(client));
        }
        return null;
    }

    private static class LoadBalancerInterceptorAccessor {

        private static final FieldAccessor requestFactory = getAccessor(LoadBalancerInterceptor.class, "requestFactory");

        private static final FieldAccessor loadBalancer = getAccessor(LoadBalancerInterceptor.class, "loadBalancer");

    }

    private static class RetryLoadBalancerInterceptorAccessor {

        private static final FieldAccessor requestFactory = getAccessor(RetryLoadBalancerInterceptor.class, "requestFactory");

        private static final FieldAccessor loadBalancer = getAccessor(RetryLoadBalancerInterceptor.class, "loadBalancer");

        private static final FieldAccessor lbProperties = getAccessor(RetryLoadBalancerInterceptor.class, "lbProperties");

        private static final FieldAccessor lbRetryFactory = getAccessor(RetryLoadBalancerInterceptor.class, "lbRetryFactory");

    }

    private static class RibbonLoadBalancerClientAccessor {

        private static final String TYPE_RIBBON_LOAD_BALANCER_CLIENT = "org.springframework.cloud.netflix.ribbon.RibbonLoadBalancerClient";

        private static final Class<?> clientType = loadClass(TYPE_RIBBON_LOAD_BALANCER_CLIENT, LoadBalancerClient.class.getClassLoader());

        private static final FieldAccessor clientFactory = getAccessor(clientType, "clientFactory");

    }

    // spring cloud 2.1.3+
    public static class BlockingLoadBalancerClientAccessor {

        protected static final String TYPE_BLOCKING_LOAD_BALANCER_CLIENT = "org.springframework.cloud.loadbalancer.blocking.client.BlockingLoadBalancerClient";

        protected static final String TYPE_REACTIVE_LOAD_BALANCER_FACTORY = "org.springframework.cloud.client.loadbalancer.reactive.ReactiveLoadBalancer$Factory";

        protected static final Class<?> clientType = loadClass(TYPE_BLOCKING_LOAD_BALANCER_CLIENT, LoadBalancerClient.class.getClassLoader());

        protected static final Class<?> factoryType = loadClass(TYPE_REACTIVE_LOAD_BALANCER_FACTORY, LoadBalancerClient.class.getClassLoader());

        protected static final FieldAccessor clientFactory = getAccessor(clientType, "loadBalancerClientFactory");

        protected static final Method getInstanceMethod = getDeclaredMethod(factoryType, "getInstance", new Class<?>[]{String.class});

        public static ReactiveLoadBalancer<ServiceInstance> getLoadBalancer(LoadBalancerClient client, String serviceId) {
            return createLoadbalancer(clientFactory.get(client), serviceId);
        }

        public static ReactiveLoadBalancer<ServiceInstance> createLoadbalancer(Object loadBalancerFactory, String serviceId) {
            try {
                return (ReactiveLoadBalancer<ServiceInstance>) getInstanceMethod.invoke(loadBalancerFactory, serviceId);
            } catch (Throwable e) {
                logger.error(e.getMessage(), e);
                return null;
            }
        }

    }
}
