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

import com.jd.live.agent.governance.registry.Registry;
import com.jd.live.agent.plugin.router.springcloud.v2_1.registry.SpringServiceRegistry;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.cloud.client.loadbalancer.reactive.ReactiveLoadBalancer;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;

import static com.jd.live.agent.bootstrap.util.type.UnsafeFieldAccessorFactory.getQuietly;

/**
 * A concrete implementation of cluster context tailored for reactive programming,
 * extending {@link AbstractCloudClusterContext}.
 */
public class ReactiveClusterContext extends BlockingClusterContext {

    private static final String FIELD_LOAD_BALANCER_CLIENT = "loadBalancerClient";
    private static final String FIELD_LOAD_BALANCER_FACTORY = "loadBalancerFactory";

    public ReactiveClusterContext(Registry registry, ExchangeFilterFunction filterFunction) {
        super(registry, null);
        // LoadBalancerExchangeFilterFunction.loadBalancerClient
        LoadBalancerClient client = getQuietly(filterFunction, FIELD_LOAD_BALANCER_CLIENT);
        if (client != null) {
            this.system = createFactory(client);
        } else {
            // LoadBalancerExchangeFilterFunction.loadBalancerFactory
            ReactiveLoadBalancer.Factory<ServiceInstance> loadBalancerFactory = getQuietly(filterFunction, FIELD_LOAD_BALANCER_FACTORY);
            if (loadBalancerFactory != null) {
                this.system = service -> new SpringServiceRegistry(service, loadBalancerFactory);
            }
        }
    }

}
