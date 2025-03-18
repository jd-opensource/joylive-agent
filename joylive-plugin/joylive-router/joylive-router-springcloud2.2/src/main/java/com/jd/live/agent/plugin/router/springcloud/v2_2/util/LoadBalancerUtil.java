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
package com.jd.live.agent.plugin.router.springcloud.v2_2.util;

import com.jd.live.agent.core.util.http.HttpMethod;
import com.jd.live.agent.governance.policy.service.cluster.RetryPolicy;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.LoadBalancerRetryProperties;
import org.springframework.cloud.client.loadbalancer.reactive.ReactiveLoadBalancer;

import java.util.HashSet;
import java.util.Set;

import static com.jd.live.agent.bootstrap.util.type.UnsafeFieldAccessorFactory.getQuietly;

/**
 * A utility class for retrieving the ReactiveLoadBalancer.Factory from a LoadBalancerClient instance.
 */
public class LoadBalancerUtil {

    private static final String TYPE_BLOCKING_LOAD_BALANCER_CLIENT = "org.springframework.cloud.loadbalancer.blocking.client.BlockingLoadBalancerClient";
    private static final String TYPE_RIBBON_LOAD_BALANCER_CLIENT = "org.springframework.cloud.netflix.ribbon.RibbonLoadBalancerClient";
    private static final String FIELD_LOAD_BALANCER_CLIENT_FACTORY = "loadBalancerClientFactory";
    private static final String TYPE_CACHING_SPRING_LOAD_BALANCER_FACTORY = "org.springframework.cloud.openfeign.ribbon.CachingSpringLoadBalancerFactory";
    private static final String FIELD_CLIENT_FACTORY = "clientFactory";
    private static final String FIELD_FACTORY = "factory";

    /**
     * Returns the ReactiveLoadBalancer.Factory associated with the given LoadBalancerClient.
     *
     * @param client the client instance
     * @return the ReactiveLoadBalancer.Factory, or null if no factory could be found
     */
    public static ReactiveLoadBalancer.Factory<ServiceInstance> getFactory(Object client) {
        if (client == null) {
            return null;
        }
        String name = client.getClass().getName();
        if (name.equals(TYPE_BLOCKING_LOAD_BALANCER_CLIENT)) {
            return getQuietly(client, FIELD_LOAD_BALANCER_CLIENT_FACTORY);
        } else if (name.equals(TYPE_RIBBON_LOAD_BALANCER_CLIENT)) {
            return new RibbonLoadBalancerFactory(getQuietly(client, FIELD_CLIENT_FACTORY));
        } else if (name.equals(TYPE_CACHING_SPRING_LOAD_BALANCER_FACTORY)) {
            return new RibbonLoadBalancerFactory(getQuietly(client, FIELD_FACTORY));
        }
        return null;
    }

    /**
     * Creates a RetryPolicy based on the provided LoadBalancerRetryProperties.
     *
     * @param properties the LoadBalancerRetryProperties object containing the retry configuration
     * @return a RetryPolicy instance, or null if retry is not enabled
     */
    public static RetryPolicy getDefaultRetryPolicy(LoadBalancerRetryProperties properties) {
        if (properties == null || !properties.isEnabled()) {
            return null;
        }
        RetryPolicy retryPolicy = new RetryPolicy();
        Set<Integer> codes = properties.getRetryableStatusCodes();
        Set<String> statuses = new HashSet<>(codes.size());
        codes.forEach(status -> statuses.add(String.valueOf(status)));
        retryPolicy.setRetry(properties.getMaxRetriesOnNextServiceInstance());
        retryPolicy.setInterval(properties.getBackoff().getMinBackoff().toMillis());
        retryPolicy.setErrorCodes(statuses);
        if (!properties.isRetryOnAllOperations()) {
            Set<String> methods = new HashSet<>(1);
            methods.add(HttpMethod.GET.name());
            retryPolicy.setMethods(methods);
        }
        return retryPolicy;
    }
}
