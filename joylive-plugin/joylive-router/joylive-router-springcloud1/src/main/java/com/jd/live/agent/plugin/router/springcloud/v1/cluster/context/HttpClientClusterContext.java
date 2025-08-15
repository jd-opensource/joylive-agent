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
import com.jd.live.agent.governance.registry.Registry;
import com.jd.live.agent.plugin.router.springcloud.v1.registry.RibbonServiceRegistry;
import lombok.Getter;
import org.springframework.cloud.client.loadbalancer.LoadBalancedRetryPolicyFactory;
import org.springframework.cloud.netflix.ribbon.apache.RetryableRibbonLoadBalancingHttpClient;
import org.springframework.cloud.netflix.ribbon.apache.RibbonLoadBalancingHttpClient;

import static com.jd.live.agent.bootstrap.util.type.FieldAccessorFactory.getAccessor;

/**
 * A concrete implementation of cluster context specifically designed for http clients,
 * extending {@link AbstractCloudClusterContext}.
 */
@Getter
public class HttpClientClusterContext extends AbstractCloudClusterContext {

    private final RibbonLoadBalancingHttpClient client;

    public HttpClientClusterContext(Registry registry, RibbonLoadBalancingHttpClient client) {
        super(registry);
        this.client = client;
        this.retryFactory = client instanceof RetryableRibbonLoadBalancingHttpClient
                ? (LoadBalancedRetryPolicyFactory) Accessor.retryFactory.get(client)
                : null;
        this.system = service -> new RibbonServiceRegistry(service, client.getLoadBalancer());
    }

    private static final class Accessor {

        private static final FieldAccessor retryFactory = getAccessor(RetryableRibbonLoadBalancingHttpClient.class, "loadBalancedRetryPolicyFactory");

    }

}
