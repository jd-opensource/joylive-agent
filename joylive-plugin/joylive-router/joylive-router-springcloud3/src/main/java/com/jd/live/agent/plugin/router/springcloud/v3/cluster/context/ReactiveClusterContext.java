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
package com.jd.live.agent.plugin.router.springcloud.v3.cluster.context;

import org.springframework.cloud.client.loadbalancer.LoadBalancerProperties;
import org.springframework.cloud.client.loadbalancer.reactive.LoadBalancedExchangeFilterFunction;
import org.springframework.cloud.client.loadbalancer.reactive.LoadBalancerClientRequestTransformer;
import org.springframework.cloud.client.loadbalancer.reactive.RetryableLoadBalancerExchangeFilterFunction;

import java.util.List;

import static com.jd.live.agent.bootstrap.util.type.UnsafeFieldAccessorFactory.getQuietly;

/**
 * A concrete implementation of cluster context tailored for reactive programming,
 * extending {@link AbstractCloudClusterContext}.
 */
public class ReactiveClusterContext extends AbstractCloudClusterContext {

    private static final String FIELD_LOAD_BALANCER_FACTORY = "loadBalancerFactory";

    private static final String FIELD_PROPERTIES = "properties";

    private static final String FIELD_TRANSFORMERS = "transformers";

    private final LoadBalancedExchangeFilterFunction filterFunction;

    private final List<LoadBalancerClientRequestTransformer> transformers;

    public ReactiveClusterContext(LoadBalancedExchangeFilterFunction filterFunction) {
        this.filterFunction = filterFunction;
        this.loadBalancerFactory = getQuietly(filterFunction, FIELD_LOAD_BALANCER_FACTORY);
        this.loadBalancerProperties = getQuietly(loadBalancerFactory, FIELD_PROPERTIES, v -> v instanceof LoadBalancerProperties);
        this.transformers = getQuietly(filterFunction, FIELD_TRANSFORMERS);
    }

    public List<LoadBalancerClientRequestTransformer> getTransformers() {
        return transformers;
    }

    @Override
    public boolean isRetryable() {
        return filterFunction instanceof RetryableLoadBalancerExchangeFilterFunction;
    }

}
