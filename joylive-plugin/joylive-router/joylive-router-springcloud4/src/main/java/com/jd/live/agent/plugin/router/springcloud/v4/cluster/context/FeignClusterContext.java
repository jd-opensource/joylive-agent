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
package com.jd.live.agent.plugin.router.springcloud.v4.cluster.context;

import feign.Client;
import org.springframework.cloud.client.loadbalancer.LoadBalancerProperties;
import org.springframework.cloud.openfeign.loadbalancer.RetryableFeignBlockingLoadBalancerClient;

import static com.jd.live.agent.bootstrap.util.type.UnsafeFieldAccessorFactory.getQuietly;

/**
 * A concrete implementation of cluster context specifically designed for Feign clients,
 * extending {@link AbstractCloudClusterContext}.
 */
public class FeignClusterContext extends AbstractCloudClusterContext {

    private static final String FIELD_DELEGATE = "delegate";

    private static final String FIELD_LOAD_BALANCER_CLIENT_FACTORY = "loadBalancerClientFactory";

    private static final String FIELD_PROPERTIES = "properties";

    private final Client client;

    private final Client delegate;

    public FeignClusterContext(Client client) {
        this.client = client;
        this.delegate = getQuietly(client, FIELD_DELEGATE);
        this.loadBalancerFactory = getQuietly(client, FIELD_LOAD_BALANCER_CLIENT_FACTORY);
        this.loadBalancerProperties = getQuietly(loadBalancerFactory, FIELD_PROPERTIES, v -> v instanceof LoadBalancerProperties);
    }

    public Client getDelegate() {
        return delegate;
    }

    @Override
    public boolean isRetryable() {
        return client instanceof RetryableFeignBlockingLoadBalancerClient;
    }
}
