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
import feign.Client;
import lombok.Getter;
import org.springframework.cloud.client.loadbalancer.LoadBalancerRetryProperties;
import org.springframework.cloud.openfeign.loadbalancer.RetryableFeignBlockingLoadBalancerClient;

import static com.jd.live.agent.bootstrap.util.type.FieldAccessorFactory.getQuietly;
import static com.jd.live.agent.plugin.router.springcloud.v2_2.cluster.context.BlockingClusterContext.createFactory;

/**
 * A concrete implementation of cluster context specifically designed for Feign clients,
 * extending {@link AbstractCloudClusterContext}.
 */
public class FeignClusterContext extends AbstractCloudClusterContext {

    private static final String FIELD_DELEGATE = "delegate";

    private static final String FIELD_LOAD_BALANCER_CLIENT = "loadBalancerClient";

    private static final String[] FIELD_RETRY_PROPERTIES = {
            "loadBalancedRetryFactory.retryProperties",
            "lbClientFactory.loadBalancedRetryFactory.retryProperties"
    };

    private final Client client;

    @Getter
    private final Client delegate;

    public FeignClusterContext(Registry registry, Client client) {
        super(registry);
        this.client = client;
        this.delegate = getQuietly(client, FIELD_DELEGATE);
        this.system = createFactory(getQuietly(client, FIELD_LOAD_BALANCER_CLIENT));
        LoadBalancerRetryProperties retryProperties = getQuietly(client, FIELD_RETRY_PROPERTIES, v -> v instanceof LoadBalancerRetryProperties);
        this.defaultRetryPolicy = getDefaultRetryPolicy(retryProperties);
    }

    @Override
    public boolean isRetryable() {
        return client instanceof RetryableFeignBlockingLoadBalancerClient;
    }
}
