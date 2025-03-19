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

import com.jd.live.agent.plugin.router.springcloud.v2_2.registry.SpringServiceRegistry;
import lombok.Getter;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.cloud.client.loadbalancer.LoadBalancerRetryProperties;
import org.springframework.cloud.client.loadbalancer.reactive.LoadBalancerClientRequestTransformer;
import org.springframework.cloud.client.loadbalancer.reactive.ReactiveLoadBalancer;
import org.springframework.cloud.client.loadbalancer.reactive.RetryableLoadBalancerExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;

import java.util.List;

import static com.jd.live.agent.bootstrap.util.type.UnsafeFieldAccessorFactory.getQuietly;
import static com.jd.live.agent.plugin.router.springcloud.v2_2.cluster.context.BlockingClusterContext.createFactory;

/**
 * A concrete implementation of cluster context tailored for reactive programming,
 * extending {@link AbstractCloudClusterContext}.
 */
@Getter
public class ReactiveClusterContext extends AbstractCloudClusterContext {

    private static final String FIELD_LOAD_BALANCER_CLIENT = "loadBalancerClient";
    private static final String FIELD_LOAD_BALANCER_FACTORY = "loadBalancerFactory";
    private static final String FIELD_TRANSFORMERS = "transformers";
    private static final String FIELD_RETRY_PROPERTIES = "retryProperties";

    private final List<LoadBalancerClientRequestTransformer> transformers;

    public ReactiveClusterContext(ExchangeFilterFunction filterFunction) {
        // LoadBalancerExchangeFilterFunction.loadBalancerClient
        LoadBalancerClient client = getQuietly(filterFunction, FIELD_LOAD_BALANCER_CLIENT);
        if (client != null) {
            this.registryFactory = createFactory(client);
        } else {
            // LoadBalancerExchangeFilterFunction.loadBalancerFactory
            ReactiveLoadBalancer.Factory<ServiceInstance> loadBalancerFactory = getQuietly(filterFunction, FIELD_LOAD_BALANCER_FACTORY);
            if (loadBalancerFactory != null) {
                this.registryFactory = service -> new SpringServiceRegistry(service, loadBalancerFactory);
            }
        }
        LoadBalancerRetryProperties retryProperties = filterFunction instanceof RetryableLoadBalancerExchangeFilterFunction ? getQuietly(filterFunction, FIELD_RETRY_PROPERTIES) : null;
        this.defaultRetryPolicy = getDefaultRetryPolicy(retryProperties);
        this.transformers = getQuietly(filterFunction, FIELD_TRANSFORMERS);
    }
}
