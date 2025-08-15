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

import com.jd.live.agent.bootstrap.util.type.FieldAccessor;
import com.jd.live.agent.governance.registry.Registry;
import com.jd.live.agent.plugin.router.springcloud.v2_1.registry.RibbonServiceRegistry;
import feign.Client;
import lombok.Getter;
import org.springframework.cloud.client.loadbalancer.LoadBalancedRetryFactory;
import org.springframework.cloud.netflix.ribbon.SpringClientFactory;
import org.springframework.cloud.openfeign.ribbon.CachingSpringLoadBalancerFactory;
import org.springframework.cloud.openfeign.ribbon.LoadBalancerFeignClient;

import static com.jd.live.agent.bootstrap.util.type.FieldAccessorFactory.getAccessor;

/**
 * A concrete implementation of cluster context specifically designed for Feign clients,
 * extending {@link AbstractCloudClusterContext}.
 */
public class FeignClusterContext extends AbstractCloudClusterContext {

    @Getter
    private final Client delegate;

    private final SpringClientFactory clientFactory;

    public FeignClusterContext(Registry registry, LoadBalancerFeignClient client) {
        super(registry);
        this.delegate = (Client) Accessor.delegate.get(client);
        this.clientFactory = (SpringClientFactory) Accessor.clientFactory.get(client);
        this.retryFactory = (LoadBalancedRetryFactory) Accessor.loadBalancedRetryFactory.get(Accessor.lbClientFactory.get(client));
        this.system = service -> new RibbonServiceRegistry(service, clientFactory);
    }

    private static final class Accessor {

        private static final FieldAccessor delegate = getAccessor(LoadBalancerFeignClient.class, "delegate");
        private static final FieldAccessor clientFactory = getAccessor(LoadBalancerFeignClient.class, "clientFactory");
        private static final FieldAccessor lbClientFactory = getAccessor(LoadBalancerFeignClient.class, "lbClientFactory");
        private static final FieldAccessor loadBalancedRetryFactory = getAccessor(CachingSpringLoadBalancerFactory.class, "loadBalancedRetryFactory");

    }
}
