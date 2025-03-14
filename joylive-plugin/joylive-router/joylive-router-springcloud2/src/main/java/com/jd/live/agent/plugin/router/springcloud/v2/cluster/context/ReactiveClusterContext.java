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
package com.jd.live.agent.plugin.router.springcloud.v2.cluster.context;

import com.jd.live.agent.plugin.router.springcloud.v2.util.LoadBalancerUtil;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.cloud.client.loadbalancer.LoadBalancerRetryProperties;
import org.springframework.cloud.client.loadbalancer.reactive.LoadBalancerClientRequestTransformer;
import org.springframework.cloud.client.loadbalancer.reactive.RetryableLoadBalancerExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;

import java.util.List;

import static com.jd.live.agent.bootstrap.util.type.UnsafeFieldAccessorFactory.getQuietly;

/**
 * A concrete implementation of cluster context tailored for reactive programming,
 * extending {@link AbstractCloudClusterContext}.
 */
public class ReactiveClusterContext extends AbstractCloudClusterContext {

    private static final String FIELD_LOAD_BALANCER = "loadBalancerClient";

    private static final String FIELD_LOAD_BALANCER_FACTORY = "loadBalancerFactory";

    private static final String FIELD_TRANSFORMERS = "transformers";

    private static final String FIELD_RETRY_PROPERTIES = "retryProperties";

    private final ExchangeFilterFunction filterFunction;

    private final List<LoadBalancerClientRequestTransformer> transformers;

    public ReactiveClusterContext(ExchangeFilterFunction filterFunction) {
        this.filterFunction = filterFunction;
        LoadBalancerClient client = getQuietly(filterFunction, FIELD_LOAD_BALANCER);
        /*
          If client is not null, it indicates that the currently intercepted class is LoadBalancerExchangeFilterFunction;
          otherwise, the intercepted class is ReactorLoadBalancerExchangeFilterFunction.
         */
        this.loadBalancerFactory = client != null ? LoadBalancerUtil.getFactory(client) : getQuietly(filterFunction, FIELD_LOAD_BALANCER_FACTORY);
        this.transformers = getQuietly(filterFunction, FIELD_TRANSFORMERS);
        LoadBalancerRetryProperties retryProperties = filterFunction instanceof RetryableLoadBalancerExchangeFilterFunction ? getQuietly(filterFunction, FIELD_RETRY_PROPERTIES) : null;
        this.defaultRetryPolicy = LoadBalancerUtil.getDefaultRetryPolicy(retryProperties);
    }

    public ExchangeFilterFunction getFilterFunction() {
        return filterFunction;
    }

    public List<LoadBalancerClientRequestTransformer> getTransformers() {
        return transformers;
    }

}
