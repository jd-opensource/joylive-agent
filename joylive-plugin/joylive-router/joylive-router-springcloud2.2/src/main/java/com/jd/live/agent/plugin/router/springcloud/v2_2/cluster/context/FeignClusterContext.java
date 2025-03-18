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

import com.jd.live.agent.governance.policy.service.cluster.RetryPolicy;
import com.jd.live.agent.plugin.router.springcloud.v2_2.util.LoadBalancerUtil;
import feign.Client;
import org.springframework.cloud.client.loadbalancer.LoadBalancerRetryProperties;
import org.springframework.cloud.openfeign.loadbalancer.RetryableFeignBlockingLoadBalancerClient;

import static com.jd.live.agent.bootstrap.util.type.UnsafeFieldAccessorFactory.getQuietly;

/**
 * A concrete implementation of cluster context specifically designed for Feign clients,
 * extending {@link AbstractCloudClusterContext}.
 */
public class FeignClusterContext extends AbstractCloudClusterContext {

    private static final String FIELD_DELEGATE = "delegate";

    private static final String[] FIELD_CLIENT_FACTORIES = {"loadBalancerClient", "lbClientFactory"};

    private static final String[] FIELD_RETRY_PROPERTIES = {
            "loadBalancedRetryFactory.retryProperties",
            "lbClientFactory.loadBalancedRetryFactory.retryProperties"
    };

    private final Client client;

    private final Client delegate;

    private final RetryPolicy defaultRetryPolicy;

    public FeignClusterContext(Client client) {
        this.client = client;
        this.delegate = getQuietly(client, FIELD_DELEGATE);
        Object loadBalancerClient = getQuietly(client, FIELD_CLIENT_FACTORIES, null);
        this.loadBalancerFactory = LoadBalancerUtil.getFactory(loadBalancerClient);
        LoadBalancerRetryProperties retryProperties = getQuietly(client, FIELD_RETRY_PROPERTIES, v -> v instanceof LoadBalancerRetryProperties);
        this.defaultRetryPolicy = LoadBalancerUtil.getDefaultRetryPolicy(retryProperties);
    }

    public Client getDelegate() {
        return delegate;
    }

    public RetryPolicy getDefaultRetryPolicy() {
        return defaultRetryPolicy;
    }

    @Override
    public boolean isRetryable() {
        return client instanceof RetryableFeignBlockingLoadBalancerClient;
    }
}
